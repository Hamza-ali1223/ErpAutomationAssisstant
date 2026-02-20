package org.dev.erpautomationassisstant.controller;

import com.microsoft.playwright.options.AriaRole;
import jakarta.servlet.http.HttpServletResponse;
import org.dev.erpautomationassisstant.services.LocalBrowserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static com.microsoft.playwright.options.AriaRole.valueOf;

@RestController
public class TestController
{

    private final ChatClient chatClient;
    private final LocalBrowserService browserService;

    public TestController(ChatClient chatClient, LocalBrowserService browserService) {
        this.chatClient = chatClient;
        this.browserService = browserService;
    }

    @GetMapping("/test")
    public String testMethod()
    {
        return chatClient.prompt()
                .user("Hi sir")
                .call()
                .content();
    }





    @GetMapping(value = "/test-screenshot", produces = MediaType.IMAGE_PNG_VALUE)
    public void testScreenshot(HttpServletResponse response) throws IOException {
        byte[] screenshot = browserService.takeScreenshot();
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename=\"screenshot.png\"");
        response.getOutputStream().write(screenshot);
    }

    @GetMapping("/click")
    public ResponseEntity testClick(@RequestParam String selector,@RequestParam(name = "role") String AriaRole)
    {
        browserService.click(valueOf(AriaRole.toUpperCase()),selector);
        return ResponseEntity.ok("Done");
    }

}
