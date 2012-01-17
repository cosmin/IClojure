package com.offbytwo.iclojure.util;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ReflectionUtilsTest {
    @Test
    public void formatParameterTypesReturnsCommaSeparatedListOfClassNames() throws Exception {
        assertEquals("Integer, String", ReflectionUtils.formatParameterTypes(Integer.class, String.class));
    }

    @Test
    public void separateStaticsReturnsStaticFieldsFirstNonStaticFieldsSecond() {
        Pair<List<Field>, List<Field>> pair = ReflectionUtils.separateByStatic(asList(TestClass.class.getFields()));

        for(Field f: pair.first) {
            assertTrue(Modifier.isStatic(f.getModifiers()));
        }

        for(Field f: pair.second) {
            assertFalse(Modifier.isStatic(f.getModifiers()));
        }
    }

    @Test
    public void separateStaticsReturnsStaticMethodsFirstNonStaticMethodsSecond() {
        Pair<List<Method>, List<Method>> pair = ReflectionUtils.separateByStatic(asList(TestClass.class.getMethods()));

        for(Method m: pair.first) {
            assertTrue(Modifier.isStatic(m.getModifiers()));
        }

        for(Method m: pair.second) {
            assertFalse(Modifier.isStatic(m.getModifiers()));
        }
    }

    static class TestClass {
        static String sa;
        static String sb;
        String a;
        String b;

        String getA() {
            return a;
        }

        String getB() {
            return b;
        }

        static String getSA() {
            return sa;
        }

        static String getSB() {
            return sb;
        }
    }
}
