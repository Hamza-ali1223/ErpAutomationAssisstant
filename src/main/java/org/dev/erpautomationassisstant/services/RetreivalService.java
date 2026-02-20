package org.dev.erpautomationassisstant.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RetreivalService
{
    @Autowired
    private VectorStore vectorStore;

    private final ChatClient chatClient;
    @Value("classpath:/prompts/rewriteprompt.st")
    private Resource rewritePrompt;

    public RetreivalService(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    //This Method will take our query, return us our Vector Store text for issue
    public String fetchStepsforIssue(String issue)
    {
        System.out.println("Fetching steps for issue " + issue);
        //Make our Template that will be sent to the LLM
        PromptTemplate rewritePromptTemplate = PromptTemplate.builder()
                .resource(rewritePrompt)
                .build();
        String rewriteInstruction = rewritePromptTemplate.render(Map.of("issue", issue));
        //Call LLM to rewrite our issue query
        String rewrittenIssue = chatClient.prompt()
                .options(ChatOptions.builder()
                        .topK(40)
                        .temperature(0.7)
                        .build())
                .user(rewriteInstruction)
                .call()
                .content();

        System.out.println("Rewritten Instruction "+rewrittenIssue);


        //Making SearcRequest to test our retreival
        SearchRequest searchRequest = SearchRequest.builder()
                .query(rewrittenIssue)
                .similarityThreshold(0.4)
                .build();

        List<Document> search = vectorStore.similaritySearch(searchRequest);

        if(search.isEmpty())
            return "No similarities found";

        return search.get(0).getText();
    }
}
