package com.salesforce.b2eclipse.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.salesforce.b2eclipse.B2eclipseTestPlugin;
import com.salesforce.b2eclipse.model.AspectOutputJars;
import com.salesforce.b2eclipse.model.AspectPackageInfo;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;

public abstract class AbstractIntellijAspectTest {
    abstract String getExpectedResourceName();

    abstract String getTestResourceName();

    @Test
    public void runTest() throws Exception {
        AspectPackageInfo expectedInfo = loadExpected();
        Assert.assertNotNull(expectedInfo);

        AspectPackageInfo testInfo = loadTest();
        Assert.assertNotNull(testInfo);
        Assert.assertEquals(expectedInfo.getKind(), testInfo.getKind());
        Assert.assertEquals(expectedInfo.getLabel(), testInfo.getLabel());
        Assert.assertEquals(expectedInfo.getMainClass(), testInfo.getMainClass());
        Assert.assertEquals(expectedInfo.getWorkspaceRelativePath(), testInfo.getWorkspaceRelativePath());

        compareStringLists(expectedInfo.getSources(), testInfo.getSources());
        compareStringLists(expectedInfo.getDeps(), testInfo.getDeps());

        compareJarLists(expectedInfo.getJars(), testInfo.getJars());
        compareJarLists(expectedInfo.getGeneratedJars(), testInfo.getGeneratedJars());
    }

    private AspectPackageInfo loadExpected() throws Exception {
        File file = locateFile(getExpectedResourceName());
        AspectPackageInfo expected = AspectPackageInfo.loadAspectFile(file);
        return expected;
    }

    public AspectPackageInfo loadTest() throws Exception {
        File file = locateFile(getTestResourceName());
        AspectPackageInfo testInfo = IntellijAspectPackageInfoLoader.loadAspectFile(file);
        return testInfo;
    }

    private File locateFile(String resourceName) throws URISyntaxException, IOException {
        Bundle bundle = Platform.getBundle(B2eclipseTestPlugin.PLUGIN_ID);
        URL fileUrl = bundle.getEntry(resourceName);
        File file = new File(FileLocator.resolve(fileUrl).toURI());
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());
        return file;
    }

    private void compareStringLists(List<String> expected, List<String> actual) {
        ArrayList<String> expectedList = new ArrayList<String>(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expectedList.size(), actual.size());
        expectedList.removeAll(actual);
        Assert.assertEquals(0, expectedList.size());

    }

    private void compareJarLists(List<AspectOutputJars> expected, List<AspectOutputJars> actual) {
        ArrayList<AspectOutputJars> expectedList = new ArrayList<AspectOutputJars>(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expectedList.size(), actual.size());
        expectedList.removeAll(actual);
        Assert.assertEquals(0, expectedList.size());
    }
}
