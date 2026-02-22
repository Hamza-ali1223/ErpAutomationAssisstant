package org.dev.erpautomationassisstant.controller;

import org.dev.erpautomationassisstant.tools.BrowserTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestTaskController {

    private final ChatClient chatClient;

    @Autowired
    private BrowserTools browserTools;

    public TestTaskController(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    @PostMapping("/test-task")
    public String testTask(@RequestBody String task) {

        // Step 1: Generate tool-oriented steps using LLM (no tools)
        String planningPrompt = """
                You are a planner for a browser automation agent.
                Convert the user's task into a short sequence of executable steps that ONLY use the available tools below.

                Tools you may reference:
                - navigate(url)
                - click(role, name)
                - type(role, name, text)
                - wait(milliseconds)
                - captureScreenshot()
                - getPageHtml()

                Rules:
                - Output ONLY a numbered list (1., 2., 3....). No headings, no extra text.
                - Every step MUST start with exactly one tool call in the format: toolName(arg=value, ...).
                - Use only the tool names listed above (case-sensitive).
                - Use double quotes for all string values.
                - Use the exact argument names:
                  - navigate(url="...")
                  - click(role="...", name="...")
                  - type(role="...", name="...", text="...")
                  - wait(milliseconds=2000)
                - Always include captureScreenshot() as step 1.
                - Keep steps minimal (aim ~5–10 steps).
                - Prefer stable ARIA selectors (role + accessible name).
                - Use getPageHtml() only if necessary.

                User task: %s
                """.formatted(task);

        System.out.println("#################### Planning Prompt Logger ##############");
        String stepsResponse = chatClient
                .prompt(planningPrompt)
                .advisors(new SimpleLoggerAdvisor())
                .call()
                .content();

        System.out.println("--------------------- Agent Prompt Logger --------------");

        // Step 2: Execution prompt with tools
        // IMPORTANT CHANGE: We do NOT attach any screenshot media to the model input.
        // Also: captureScreenshot tool no longer returns base64; it saves to disk and returns a tiny JSON string.
        String executionPrompt = """
                You are an autonomous agent. Your goal is to accomplish the following task using the provided tools.

                Task: %s

                Follow these steps:
                %s

                You have access to these tools:
                - captureScreenshot(): saves screenshot to ./screenshots/screenshots.png and returns JSON with the path
                - click(role, name)
                - type(role, name, text)
                - navigate(url)
                - getPageHtml()
                - wait(milliseconds)

                Important:
                - Begin by calling captureScreenshot().
                - After each action (navigate/click/type), you MAY call captureScreenshot() if needed for verification.
                - When you believe the task is complete, respond with: TASK_COMPLETE: <summary>
                - If you get stuck, explain why.

                Begin now.
                """.formatted(task, stepsResponse);

        UserMessage userMessage = UserMessage.builder()
                .text(executionPrompt)
                .build();

        String result = chatClient
                .prompt(new Prompt(userMessage))
                .advisors(new SimpleLoggerAdvisor())
                .tools(browserTools)
                .call()
                .content();

        return result;
    }
}