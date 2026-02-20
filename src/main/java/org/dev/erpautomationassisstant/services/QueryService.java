package org.dev.erpautomationassisstant.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class QueryService
{
    @Value("classpath:/prompts/queryprompt.st")
    private Resource queryPrompt;

    private final RetreivalService retreivalService;
    private final ChatClient chatClient;
    public QueryService(RetreivalService retreivalService, ChatClient.Builder chatClient) {
        this.retreivalService = retreivalService;
        this.chatClient = chatClient.build();
    }

    //Method that will take issue and return steps for our context
    public String resolveIssue(String issue)
    {
        //Step 01: Fetch our Steps
        String fetchedStepsforIssue = retreivalService.fetchStepsforIssue(issue);

        //Step 02: Make our PromptTemplate for our ChatClient
        PromptTemplate queryTemplate = new PromptTemplate(queryPrompt);

        //Step 03: Render our Template
        String queryWithContext = queryTemplate.render(Map.of("our_issue", issue, "retrieved_steps", fetchedStepsforIssue));

        //Step 04: Calling our LLM
        String resultSteps = chatClient.prompt()
                .options(ChatOptions.builder()
                        .temperature(0.4)
                        .build())
                .user(queryWithContext)
                .call()
                .content();

        return resultSteps;
    }
}
