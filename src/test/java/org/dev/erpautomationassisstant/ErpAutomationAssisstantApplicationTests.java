package org.dev.erpautomationassisstant;

import org.dev.erpautomationassisstant.services.IngestionService;
import org.dev.erpautomationassisstant.services.QueryService;
import org.dev.erpautomationassisstant.services.RetreivalService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;


@SpringBootTest
public class ErpAutomationAssisstantApplicationTests {


    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private RetreivalService retreivalService;

    @Autowired
    private QueryService queryService;
    @Test
    void testEmbedAndStore() {
        Assert.isTrue(ingestionService.LoadDocumentInVectorStore(),"Data Loaded hehe");
    }

    @Test
    void testQuery()
    {
        System.out.println(queryService.resolveIssue("After updating Samantha Alexander’s preferred language and saving the profile, it automatically switches back to the previous language."));
    }
}