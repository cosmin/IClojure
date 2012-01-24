package com.offbytwo.iclojure;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class InputOutputCacheTest {

    @Test
    public void elementsIndexedStartingWithOne() {
        InputOutputCache cache = new InputOutputCache(1);
        cache.add("i", "o");
        assertEquals("i", cache.getInput(1));
        assertEquals("o", cache.getOutput(1));
    }

    @Test
    public void addMoreElementsThanTheSize() {
        InputOutputCache cache = getCacheWithSize2And3Elements();

        assertEquals("i2", cache.getInput(2));
        assertEquals("o2", cache.getOutput(2));

        assertEquals("i3", cache.getInput(3));
        assertEquals("o3", cache.getOutput(3));
    }

    @Test
    public void getFirstCachedIndexWithMoreElementsThanSize() {
        InputOutputCache cache = getCacheWithSize2And3Elements();
        assertEquals(2, cache.getFirstCachedIndex());
    }

    @Test
    public void getFirstCachedIndexWithLessElementsThanSize() {
        InputOutputCache cache = new InputOutputCache(2).add("i1", "o1").add("i2", "o2");
        assertEquals(1, cache.getFirstCachedIndex());
    }

    @Test
    public void getLastCachedIndex() {
        InputOutputCache cache = getCacheWithSize2And3Elements();
        assertEquals(3, cache.getLastCachedIndex());
    }

    @Test
    public void getSize() {
        InputOutputCache cache = new InputOutputCache(2);
        assertEquals(2, cache.getSize());
    }

    private InputOutputCache getCacheWithSize2And3Elements() {
        InputOutputCache cache = new InputOutputCache(2);
        cache.add("i1", "o1");
        cache.add("i2", "o2");
        cache.add("i3", "o3");
        return cache;
    }
}
