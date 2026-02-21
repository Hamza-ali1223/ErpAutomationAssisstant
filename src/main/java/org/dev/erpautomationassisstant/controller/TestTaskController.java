package org.dev.erpautomationassisstant.controller;

import org.dev.erpautomationassisstant.tools.BrowserTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
public class TestTaskController {


    private final ChatClient chatClient; // your configured Gemini ChatClient

    @Autowired
    private BrowserTools browserTools;

    public TestTaskController(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    @PostMapping("/test-task")
    public String testTask(@RequestBody String task) {
        // Step 1: Generate steps using LLM without tools
        String planningPrompt = """
                You are a planner for a browser automation agent.
                
                         Convert the user's task into a short sequence of executable steps that ONLY use the available tools below:
                
                         Tools you may reference:
                         - navigate(url)
                         - click(role, name)
                         - type(role, name, text)
                         - wait(ms)
                         - captureScreenshot()
                         - getPageHtml()
                
                         Rules:
                         - Output ONLY a numbered list (1., 2., 3....). No headings, no extra text.
                         - Every step MUST start with exactly one tool call in the format: toolName(arg=value, ...).
                         - Use only the tool names listed above (case-sensitive).
                         - Use double quotes for all string values.
                         - Keep steps minimal (aim ~6–12 steps).
                         - Always include captureScreenshot() as step 1.
                         - After any navigate/click/type, include a captureScreenshot() step to verify.
                         - Prefer stable ARIA selectors for click/type: role and accessible name. Use common roles like "link", "button", "textbox", "combobox".
                         - If you cannot confidently identify what to click/type next from the screenshot alone, add getPageHtml() (then a captureScreenshot()) and proceed using information from the HTML.
                         - Use wait(ms) only when needed (e.g., after navigation or submitting a search).
                
                         User task: %s
                """.formatted(task);
        System.out.println("#################### Planning Prompt Logger ##############");
        String stepsResponse = chatClient.prompt(planningPrompt)
                .options(ChatOptions.builder()
                        .build())
                .advisors(new SimpleLoggerAdvisor()).call().content();
        System.out.println("--------------------- Agent Prompt Logger --------------");
        // Step 2: Capture initial screenshot
        String initialScreenshot = browserTools.captureScreenshot();
        if(initialScreenshot.startsWith("Error"))
            return "Initial ScreenShot Failed "+initialScreenshot;

        // Step 3: Execution prompt with tools
        String executionPrompt = """
                You are an autonomous agent. Your goal is to accomplish the following task using the provided tools.

                Task: %s

                Follow these steps:
                %s

                You have access to these tools:
                - captureScreenshot: returns a base64 image of the current page
                - click(role, name): click an element by its ARIA role and accessible name
                - type(role, name, text): type into a field
                - navigate(url): go to a URL
                - getPageHtml: get page HTML
                - wait(ms): pause for milliseconds

                Important:
                - Always start by observing the page with captureScreenshot.
                - After each action, call captureScreenshot again to verify the result.
                - When you believe the task is complete, respond with "TASK_COMPLETE: <summary>".
                - If you get stuck, explain why.

                Begin now.
                """.formatted(task, stepsResponse);

        byte[] decoded = Base64.getDecoder().decode(initialScreenshot);

        // Create user message with text and initial screenshot
        UserMessage userMessage = UserMessage.builder().text(executionPrompt).media(new Media(MimeTypeUtils.IMAGE_PNG,
                new ByteArrayResource(decoded))).build();
        // Execute with tools
        String result = chatClient.prompt(new Prompt(userMessage))
                .advisors(new SimpleLoggerAdvisor())
                .tools(browserTools)
                .call()
                .content();

        return result;
    }
}