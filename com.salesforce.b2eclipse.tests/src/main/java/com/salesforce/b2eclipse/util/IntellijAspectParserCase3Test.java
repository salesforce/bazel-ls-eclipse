package com.salesforce.b2eclipse.util;

import com.salesforce.b2eclipse.model.AspectPackageInfo;

import org.junit.Assert;
import org.junit.Test;

public class IntellijAspectParserCase3Test extends AbstractIntellijAspectTest {
    @Override
    String getExpectedResourceName() {
        return null;
    }

    @Override
    String getTestResourceName() {
        return "/intellij/test03-test.intellij-info.txt";
    }
    
    @Test
    public void runTest() throws Exception {
        AspectPackageInfo testInfo = loadTest();
        Assert.assertNull(testInfo);
    }
}
