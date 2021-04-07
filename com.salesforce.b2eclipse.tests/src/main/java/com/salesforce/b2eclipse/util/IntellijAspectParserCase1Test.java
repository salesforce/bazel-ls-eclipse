package com.salesforce.b2eclipse.util;

public class IntellijAspectParserCase1Test extends AbstractIntellijAspectTest {
    @Override
    String getExpectedResourceName() {
        return "/intellij/test01-expected.bzleclipse-build.json";
    }

    @Override
    String getTestResourceName() {
        return "/intellij/test01-test.intellij-info.txt";
    }
}
