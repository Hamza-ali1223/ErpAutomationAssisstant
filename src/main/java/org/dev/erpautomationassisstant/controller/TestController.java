package org.dev.erpautomationassisstant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController
{

    private final ChatClient chatClient;

    public TestController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/test")
    public String testMethod()
    {
        return chatClient.prompt()
                .user("Hi sir")
                .call()
                .content();
    }







}
