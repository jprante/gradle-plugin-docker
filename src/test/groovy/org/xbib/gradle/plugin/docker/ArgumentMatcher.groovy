package org.xbib.gradle.plugin.docker

import org.hamcrest.BaseMatcher
import org.hamcrest.Description

import java.util.regex.Matcher
import java.util.regex.Pattern;

abstract class ArgumentMatcher<T> extends BaseMatcher<T> {

    abstract boolean matches(Object var1);

    void describeTo(Description description) {
        String className = this.getClass().getSimpleName()
        description.appendText(decamelizeMatcher(className))
    }

    static String decamelizeMatcher(String className) {
        if (className.length() == 0) {
            return "<custom argument matcher>"
        } else {
            String decamelized = decamelizeClassName(className)
            return decamelized.length() == 0 ? "<" + className + ">" : "<" + decamelized + ">"
        }
    }

    private static final Pattern CAPS = Pattern.compile("([A-Z\\d][^A-Z\\d]*)")

    private static String decamelizeClassName(String className) {
        Matcher match = CAPS.matcher(className)
        StringBuilder deCameled = new StringBuilder()
        while (match.find()) {
            if (deCameled.length() == 0) {
                deCameled.append(match.group())
            } else {
                deCameled.append(" ")
                deCameled.append(match.group().toLowerCase())
            }
        }
        return deCameled.toString()
    }
}