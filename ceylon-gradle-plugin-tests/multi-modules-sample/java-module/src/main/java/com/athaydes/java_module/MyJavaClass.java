package com.athaydes.java_module;

import com.google.common.base.Strings;

public class MyJavaClass {

    private final String message;

    public MyJavaClass(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getMessagePaddedLeft(int minLength, char padChar) {
        return Strings.padStart(message, minLength, padChar);
    }

}