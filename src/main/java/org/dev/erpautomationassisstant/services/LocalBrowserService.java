package org.dev.erpautomationassisstant.services;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
public class LocalBrowserService
{
    private Playwright playwright;
    private Browser browser;
    private Page page;




    @PostConstruct
    public void init()
    {

        //Now we are going to connect to our current running Chrome Instance on Port 9222
        playwright = Playwright.create();
        browser = playwright.chromium().connectOverCDP("http://localhost:9222");
        //Lets Print the Browser Properties
        System.out.println(browser.browserType().name());
        System.out.println(browser.isConnected());
        System.out.println(browser.contexts().toString());

        //Assume the first page in the default context is the one i would want
        BrowserContext browserContext = browser.contexts().get(0);

        //List our Pages

        Page desiredPage = browserContext.pages().stream().filter(page ->
        {
            String titleToSearch = "web";
            String title = page.title().toLowerCase();
            boolean matches = title != null && title.contains(titleToSearch.toLowerCase());

            return matches;
        }).findFirst().orElse(null);

        page = desiredPage;

        System.out.println("Connected to our Chrome: current URL: "+ page.url());
        //First Print the class Names
        System.out.println(playwright.getClass().getName());
        System.out.println(browser.getClass().getName());
        System.out.println(page.getClass().getName());

    }
    public byte[] takeScreenshot() {
        return page.screenshot();
    }

    public void click(AriaRole role,String selector) {
       //we will use getByrole to fix the Click issue
        page.getByRole(role, new Page.GetByRoleOptions().setName(selector).setExact(true))
                .click();    }
    @PreDestroy
    public void cleanup() {
        if (playwright != null) {
            playwright.close(); // This closes the connection, but NOT your Chrome window
        }
    }
}
