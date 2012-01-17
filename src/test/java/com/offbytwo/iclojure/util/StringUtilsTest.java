package com.offbytwo.iclojure.util;

import org.junit.Test;

import static com.offbytwo.iclojure.util.StringUtils.join;
import static junit.framework.Assert.assertEquals;

public class StringUtilsTest {
    @Test
    public void joinOfEmptyArrayReturnsEmptyString() throws Exception {
        String[] emptyArray = new String[0];
        assertEquals("", join("-", (Object[])emptyArray));
    }

    @Test
    public void joinOfSingleElementArrayReturnsSingleElementString() {
        String content = "foobarbaz";
        String[] oneElementArray = new String[] {content};
        assertEquals(content, join("-", (Object[])oneElementArray));
    }

    @Test
    public void joinOfMultipleElementArrayReturnsCompomentsJoinedBySeparator() {
        String el1 = "a";
        String el2 = "b";
        String [] data = new String[] {el1, el2};
        assertEquals("a-b", join("-", (Object[])data));

    }
}
