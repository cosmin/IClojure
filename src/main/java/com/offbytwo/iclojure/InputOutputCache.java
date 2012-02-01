package com.offbytwo.iclojure;

import clojure.lang.AFn;

public class InputOutputCache {
    Object[] inputCache;
    Object[] outputCache;

    int size;
    int lastElementIndex = 0;
    int insertAt = 0;

    public InputOutputCache(int size) {
        this.size = size;
        inputCache = new Object[size];
        outputCache = new Object[size];
    }


    private int convertToInteger(Object arg1) {
        int value;
        if (arg1 instanceof Long) {
            value = ((Long) arg1).intValue();
        } else if (arg1 instanceof Integer) {
            value = (Integer) arg1;
        } else {
            throw new IllegalArgumentException("Argument must be Integer or Long. Received: " + arg1.getClass());
        }
        return value;
    }

    public AFn getInputLookupFn() {
        final InputOutputCache cache = this;
        return new AFn() {
            @Override
            public Object invoke(Object arg1) {
                return cache.getInput(convertToInteger(arg1));
            }
        };
    }

    public AFn getOutputLookupFn() {
        final InputOutputCache cache = this;
        return new AFn() {
            @Override
            public Object invoke(Object arg1) {
                return cache.getOutput(convertToInteger(arg1));
            }
        };
    }

    public InputOutputCache add(Object input, Object output) {
        lastElementIndex += 1;

        inputCache[insertAt] = input;
        outputCache[insertAt] = output;

        insertAt += 1;
        if (insertAt >= inputCache.length) {
            insertAt = 0;
        }

        return this;
    }

    private int getActualIndex(int requestedIndex) {
        int elementsAgo = lastElementIndex - requestedIndex;
        int actualIndex = insertAt - elementsAgo - 1;

        if (actualIndex < 0) {
            actualIndex = size + actualIndex;
            if (actualIndex < insertAt) {
                throw new IndexOutOfBoundsException("requested index is more than " + size + " elements back");
            }
        }

        return actualIndex;
    }

    public Object getInput(int index) {
        return inputCache[getActualIndex(index)];
    }

    public Object getOutput(int index) {
        return outputCache[getActualIndex(index)];
    }


    public int getFirstCachedIndex() {
        if (lastElementIndex > size) {
            return lastElementIndex - size + 1;
        } else {
            return 1;
        }
    }


    public int getLastCachedIndex() {
        return lastElementIndex;
    }

    public int getSize() {
        return size;
    }
}
