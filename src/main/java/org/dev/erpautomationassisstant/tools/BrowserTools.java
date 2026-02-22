package org.dev.erpautomationassisstant.tools;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class BrowserTools {

    @Autowired
    private Page page;

    // As requested: project root / screenshots / screenshots.png
    private static final Path SCREENSHOT_PATH = Paths.get("screenshots", "screenshots.png");

    @Tool(
            name = "captureScreenshot",
            description = "Capture a screenshot and save it to project root ./screenshots/screenshots.png. Returns JSON with the saved path."
    )
    public String captureScreenshot() {
        try {
            byte[] screenshot = page.screenshot(); // default: viewport PNG
            Files.createDirectories(SCREENSHOT_PATH.getParent());
            Files.write(SCREENSHOT_PATH, screenshot);

            System.out.println("captureScreenshot tool is called 💻 saved: " + SCREENSHOT_PATH.toAbsolutePath());

            long bytes = Files.size(SCREENSHOT_PATH);
            return "{\"saved\":true,\"path\":\"" + SCREENSHOT_PATH.toString().replace("\\", "/") + "\",\"bytes\":" + bytes + "}";
        } catch (Exception e) {
            return "Error taking screenshot: " + e.getMessage();
        }
    }

    @Tool(name = "click", description = "Click an element identified by ARIA role and accessible name")
    public String click(
            @ToolParam(description = "The ARIA role of the element, e.g., 'button', 'link', 'checkbox'") String role,
            @ToolParam(description = "The accessible name of the element") String name
    ) {
        try {
            System.out.println("Trying ARIA role click...");

            AriaRole ariaRole = AriaRole.valueOf(role.toUpperCase());
            var locator = page.getByRole(ariaRole, new Page.GetByRoleOptions().setName(name));

            if (locator.count() > 0) {
                locator.first().click();
                return "Clicked using ARIA role selector.";
            }

            System.out.println("Fallback: Trying text selector...");
            var textLocator = page.getByText(name);
            if (textLocator.count() > 0) {
                textLocator.first().click();
                return "Clicked using text selector.";
            }

            System.out.println("Fallback: Trying link text...");
            var linkLocator = page.locator("a:has-text('" + name + "')");
            if (linkLocator.count() > 0) {
                linkLocator.first().click();
                return "Clicked using link text selector.";
            }

            return "No clickable element found.";

        } catch (Exception e) {
            return "Failed to click: " + e.getMessage();
        }
    }

    @Tool(name = "type", description = "Type text into a field identified by ARIA role and accessible name")
    public String type(
            @ToolParam(description = "The ARIA role of the input field, usually 'textbox'") String role,
            @ToolParam(description = "The accessible name of the field (e.g., 'Username', 'Search')") String name,
            @ToolParam(description = "The text to type into the field") String text
    ) {
        try {
            System.out.println("Type Tool Called → role: " + role + ", name: " + name);

            com.microsoft.playwright.Locator locator = null;

            // 1️⃣ Try ARIA role + accessible name
            try {
                AriaRole ariaRole = AriaRole.valueOf(role.toUpperCase());
                var byRole = page.getByRole(ariaRole, new Page.GetByRoleOptions().setName(name));
                if (byRole.count() > 0) {
                    locator = byRole.first();
                    System.out.println("Matched using ARIA role selector");
                }
            } catch (Exception ignored) {}

            // 2️⃣ Try label match
            if (locator == null) {
                var byLabel = page.getByLabel(name);
                if (byLabel.count() > 0) {
                    locator = byLabel.first();
                    System.out.println("Matched using label selector");
                }
            }

            // 3️⃣ Try placeholder contains name
            if (locator == null) {
                var byPlaceholder = page.locator("input[placeholder*=\"" + escape(name) + "\"]");
                if (byPlaceholder.count() > 0) {
                    locator = byPlaceholder.first();
                    System.out.println("Matched using placeholder selector");
                }
            }

            // 4️⃣ Try common search patterns (Google/Wikipedia)
            if (locator == null) {
                String[] commonSelectors = {
                        "input[name='q']",
                        "input[type='search']",
                        "input[aria-label*='Search']",
                        "input[type='text']"
                };

                for (String sel : commonSelectors) {
                    var loc = page.locator(sel);
                    if (loc.count() > 0) {
                        locator = loc.first();
                        System.out.println("Matched using fallback selector: " + sel);
                        break;
                    }
                }
            }

            if (locator == null) {
                return "Failed to type: No matching input field found.";
            }

            // 5️⃣ Ensure visible + interactable
            locator.scrollIntoViewIfNeeded();
            locator.waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));

            // 6️⃣ Focus before typing
            locator.click(new com.microsoft.playwright.Locator.ClickOptions().setTimeout(5000));

            // 7️⃣ Clear existing value safely
            locator.fill("");

            // 8️⃣ Type instead of fill (more realistic, avoids some JS issues)
            locator.type(text, new com.microsoft.playwright.Locator.TypeOptions().setDelay(20));

            return "Typed '" + text + "' successfully.";

        } catch (Exception e) {
            return "Failed to type: " + e.getMessage();
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Tool(name = "navigate", description = "Navigate to a relative or absolute URL")
    public String navigate(
            @ToolParam(description = "The URL to navigate to (can be relative like '/setup' or absolute like 'https://example.com')") String url
    ) {
        try {
            page.navigate(url);
            System.out.println("navigate Tool is Called with URL: " + url);
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
            @ToolParam(description = "Number of milliseconds to wait (e.g., 2000 for 2 seconds)") int milliseconds
    ) {
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