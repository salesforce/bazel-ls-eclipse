package com.salesforce.b2eclipse.util;

public class IntellijAspectParserCase2Test extends AbstractIntellijAspectTest {
    @Override
    String getExpectedResourceName() {
        return "/intellij/test02-expected.bzleclipse-build.json";
    }

    @Override
    String getTestResourceName() {
        return "/intellij/test02-test.intellij-info.txt";
    }
}
