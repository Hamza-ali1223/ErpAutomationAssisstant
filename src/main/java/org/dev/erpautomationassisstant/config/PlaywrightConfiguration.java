package org.dev.erpautomationassisstant.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaywrightConfiguration
{

    @Bean
    public Page page()
    {
        //First create playwright object
        Playwright playwright = Playwright.create();

       var browser = playwright.chromium().connectOverCDP("http://localhost:9222");

        BrowserContext browserContext = browser.contexts().get(0);

        Page desiredPage = browserContext.pages().stream().filter(
                page -> {
                    String titleToSearch = "youtube".toLowerCase();
                    String title = page.title().toLowerCase();
                    boolean matches = title != null && title.contains(titleToSearch);
                    return matches;
                }
        ).findFirst().orElse(null);


        if(desiredPage != null)
        {
            return desiredPage;
        }

        return null;
    }

//    @Bean
//    public BrowserContext browserContext()
//    {
//        //First create playwright object
//        Playwright playwright = Playwright.create();
//
//        var browser = playwright.chromium().connectOverCDP("http://localhost:9222");
//
//        BrowserContext browserContext = browser.contexts().get(0);
//
//        if(browserContext != null)
//        {
//            return browserContext;
//        }
//        return null;
//    }
}
