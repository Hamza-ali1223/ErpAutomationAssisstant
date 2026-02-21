package org.dev.erpautomationassisstant.tools;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class BrowserTools {

    @Autowired
    private Page page;

    @Tool(name = "captureScreenshot", description = "Take a screenshot of the current page and return as base64")
    public String captureScreenshot() {
        try {
            byte[] screenshot = page.screenshot();
            System.out.println("captureScreenshot tool is called 💻");
            return Base64.getEncoder().encodeToString(screenshot);
        } catch (Exception e) {
            return "Error taking screenshot: " + e.getMessage();
        }
    }

    @Tool(name = "click", description = "Click an element identified by ARIA role and accessible name")
    public String click(
            @ToolParam(description = "The ARIA role of the element, e.g., 'button', 'link', 'checkbox'") String role,
            @ToolParam(description = "The accessible name of the element (from aria-label, text content, or alt text)") String name) {
        try {
            AriaRole ariaRole = AriaRole.valueOf(role.toUpperCase());
            page.getByRole(ariaRole, new Page.GetByRoleOptions().setName(name)).click();
            System.out.println("Click Tool is Called with AriaRole: "+role + "element Name: "+name);
            return "Clicked " + role + " with name '" + name + "'";
        } catch (IllegalArgumentException e) {
            return "Invalid role: " + role + ". Valid roles include: button, link, textbox, checkbox, etc.";
        } catch (Exception e) {
            return "Failed to click: " + e.getMessage();
        }
    }

    @Tool(name = "type", description = "Type text into a field identified by ARIA role and accessible name")
    public String type(
            @ToolParam(description = "The ARIA role of the input field, usually 'textbox'") String role,
            @ToolParam(description = "The accessible name of the field (e.g., 'Username', 'Search')") String name,
            @ToolParam(description = "The text to type into the field") String text) {
        try {
            AriaRole ariaRole = AriaRole.valueOf(role.toUpperCase());
            page.getByRole(ariaRole, new Page.GetByRoleOptions().setName(name)).fill(text);
            System.out.println("type Tool is Called with AriaRole: "+role + "element Name: "+name);
            return "Typed '" + text + "' into " + role + " with name '" + name + "'";
        } catch (IllegalArgumentException e) {
            return "Invalid role: " + role + ". For text input, use 'textbox'.";
        } catch (Exception e) {
            return "Failed to type: " + e.getMessage();
        }
    }

    @Tool(name = "navigate", description = "Navigate to a relative or absolute URL")
    public String navigate(
            @ToolParam(description = "The URL to navigate to (can be relative like '/setup' or absolute like 'https://example.com')") String url) {
        try {
            page.navigate(url);
            System.out.println("navigate Tool is Called with URL: "+url);
            return "Navigated to " + url;
        } catch (Exception e) {
            return "Failed to navigate: " + e.getMessage();
        }
    }

    @Tool(name = "getPageHtml", description = "Get the HTML source of the current page")
    public String getPageHtml() {
        try {
            System.out.println("Get Page HTML Tool is Called");
            return page.content();
        } catch (Exception e) {
            return "Error getting HTML: " + e.getMessage();
        }
    }

    @Tool(name = "wait", description = "Wait for a specified number of milliseconds")
    public String wait(
            @ToolParam(description = "Number of milliseconds to wait (e.g., 2000 for 2 seconds)") int milliseconds) {
        try {
            System.out.println("Wait Tool is Called");
            Thread.sleep(milliseconds);
            return "Waited " + milliseconds + "ms";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Wait interrupted";
        }
    }
}