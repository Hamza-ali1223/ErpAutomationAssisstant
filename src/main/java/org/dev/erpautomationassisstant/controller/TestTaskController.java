package org.dev.erpautomationassisstant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestTaskController {

    private final ChatClient chatClient;

    @Autowired
    private SyncMcpToolCallbackProvider mcpToolProvider;

    public TestTaskController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping("/test-task")
    public String testTask(@RequestBody String task) {
        String promptText = """
                You are an autonomous browser automation agent. Your goal is to accomplish the following task using the provided tools.

                Task: %s

                You have a set of browser automation tools at your disposal. Use them step by step and only when i specify to do a task.
                If i have a query that then answer me and you can give information about your tools too in it
                When you believe the task is complete, respond with "TASK_COMPLETE: <summary>".
                If you get stuck, explain why.

                Begin now.
                """.formatted(task);

        UserMessage userMessage = UserMessage.builder()
                .text(promptText)
                .build();

        String result = chatClient
                .prompt(new Prompt(userMessage))
                .toolCallbacks(mcpToolProvider)
                .advisors(new SimpleLoggerAdvisor())
                .call()
                .content();

        return result;
    }
}