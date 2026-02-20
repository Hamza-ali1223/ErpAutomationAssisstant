package org.dev.erpautomationassisstant.services;

import lombok.extern.slf4j.Slf4j;
import org.dev.erpautomationassisstant.model.TaskImage;
import org.springframework.ai.document.ContentFormatter;
import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
public class IngestionService
{
    @Value("classpath:SuccessFactors_Scenarios.docx")
    private  Resource resource;

    private static final String TASK_DELIMITER="ENDOFTASK";

    private static final Pattern TASK_NUMBER_PATTERN = Pattern.compile("(\\d+)\\)\\s+([^\n]+)");

    @Autowired
    private MariaDBVectorStore mariaDBVectorStore;



    public boolean LoadDocumentInVectorStore()
    {
        if(resource==null)
            return false;

      log.info("Loading our Doc inside Vector Store");
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

        //Step 01: Read our Docx file
        List<Document> documentList = tikaDocumentReader.read();


        log.info("✅ Read {} raw document(s)", documentList.size());

        //Step 02: Split our Document into sub chunks based on our Delimiter
        List<Document> taskChunks = splitByTaskDelimter(documentList);

        log.info("✅ Split into {} task chunks", taskChunks.size());

        //Step 03: Filter our images_.png text from last document
        List<Document> fitleredDocuments = stripImageNoiseFromLastChunk(taskChunks);

        //Last Step: Storing our StrippedImagesNoisefromLastChunk List into our Vector Store
        mariaDBVectorStore.add(fitleredDocuments);
        log.info("✅ Ingestion complete! {} tasks ready for RAG", taskChunks.size());

        return true;
    }


    private List<Document> splitByTaskDelimter(List<Document> documentList)
    {
        List<Document> documents = new ArrayList<>();

        for(Document doc : documentList)
        {
            String content = doc.getText();

            //Split by our Delimiter
            String[] taskParts = content.split(TASK_DELIMITER);

            log.info(" 💉 Found our Task Parts :"+taskParts.length);

            for(int i=0;i<taskParts.length;i++)
            {
                String taskTest = taskParts[i].trim();

                //Skips Empty Tasks
                if(taskTest.isEmpty())
                    continue;


                TaskInfo extractedTaskInfo = extractTaskInfo(taskTest, i);

                List<String> extractedImageReferences = extractImageReferences(taskTest);

                Document taskChunk = new Document(
                        UUID.randomUUID().toString(),
                        taskTest,
                        mergeMetadata(doc.getMetadata(),extractedTaskInfo)
                );

                documents.add(taskChunk);

            }
        }

        return documents;
    }

    //Our Method for extracing Task Info
    private TaskInfo extractTaskInfo(String taskText, int index)
    {
        Matcher matcher = TASK_NUMBER_PATTERN.matcher(taskText);

        if(matcher.find())
        {
            String taskNumber = matcher.group(1);
            String taskTitle = matcher.group(2).split("\n")[0].trim();
            return new TaskInfo(taskNumber,taskTitle);
        }

        //Fallback if pattern doesnt match
        return new TaskInfo(String.valueOf(index+1),"Task "+(index+1));

    }

    //Our Method for extracing images filenames referenced in our Task Text
    private List<String> extractImageReferences(String taskText) {
        List<String> images = new ArrayList<>();

        // Pattern to match image*.png references
        Pattern imagePattern = Pattern.compile("image\\d+\\.png", Pattern.CASE_INSENSITIVE);
        Matcher matcher = imagePattern.matcher(taskText);

        while (matcher.find()) {
            images.add(matcher.group());
        }

        return images;
    }

    //Merges Original Metadata with Task Specific Metadata
    private java.util.Map<String, Object> mergeMetadata(
            java.util.Map<String, Object> original,
            TaskInfo taskInfo
    ) {
        java.util.Map<String, Object> merged = new java.util.HashMap<>(original);
        merged.put("task_number", taskInfo.taskNumber());
        merged.put("task_title", taskInfo.taskTitle());
        merged.put("chunk_type", "task");
        merged.put("source_file", resource.getFilename());
        merged.put("ingested_at", System.currentTimeMillis());
        return merged;
    }


    /**
     * Cleans the last document chunk by removing the list of image filenames (e.g., "image1.png").
     *
     * @param taskChunks The list of document chunks where the last one contains the image noise.
     * @return A new List<Document> with the last chunk's text cleaned.
     */
    public List<Document> stripImageNoiseFromLastChunk(List<Document> taskChunks) {
        if (taskChunks == null || taskChunks.isEmpty()) {
            return taskChunks;
        }

        // Create a new list to avoid mutating the original input directly
        List<Document> cleanChunks = new ArrayList<>(taskChunks);

        // Get the last document
        Document lastDoc = cleanChunks.get(cleanChunks.size() - 1);

        String originalText = lastDoc.getText();

        // Regex to match "image" + numbers + ".png" (case insensitive)
        // This handles "image3.png", "image61.png", etc., even if separated by newlines
        Pattern imagePattern = Pattern.compile("(?m)^image\\d+\\.png\\s*$");

        // Replace all matches with an empty string
        String cleanedText = imagePattern.matcher(originalText).replaceAll("");

        // Optional: Clean up excessive newlines left behind by the removal
        cleanedText = cleanedText.replaceAll("\\n{3,}", "\n\n").trim();

        // Create a NEW Document instance with the cleaned text but keep existing metadata
        Document cleanLastDoc = new Document(
                lastDoc.getId(),
                cleanedText,
                lastDoc.getMetadata()
        );

        // Replace the last item in our new list
        cleanChunks.set(cleanChunks.size() - 1, cleanLastDoc);

        System.out.println("✅ Stripped image filenames from last chunk. Original length: "
                + originalText.length() + ", Cleaned length: " + cleanedText.length());

        return cleanChunks;
    }


    //Record of our TaskInfo
    public record TaskInfo(String taskNumber, String taskTitle){}
}
