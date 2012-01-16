package com.offbytwo.iclojure.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.offbytwo.iclojure.util.StringUtils.join;

class Pair<A,B> {
    public A first;
    public B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
}

public class ReflectionUtils {

    public static String formatParameterTypes(Class... parameterTypes) {
        List<String> classNames = new ArrayList<String>();

        for (Class c : parameterTypes) {
            classNames.add(c.getSimpleName());
        }

        return join(", ", classNames.toArray());
    }


    public static <T> List<T> sortStaticsFirst(List<T> all) {
        Pair<List<T>, List<T>> staticsAndNonStatics = separateByStatic(all);
        List<T> combined = new ArrayList<T>();
        combined.addAll(staticsAndNonStatics.first);
        combined.addAll(staticsAndNonStatics.second);

        return combined;
    }

    static <T> Pair<List<T>, List<T>> separateByStatic(List<T> all) {
        List<T> statics = new ArrayList<T>();
        List<T> nonStatics = new ArrayList<T>();

        for (T el : all) {
            if (el instanceof Method) {
                if (Modifier.isStatic(((Method) el).getModifiers())) {
                    statics.add(el);
                } else {
                    nonStatics.add(el);
                }
            } else if (el instanceof Field) {
                if (Modifier.isStatic(((Field) el).getModifiers())) {
                    statics.add(el);
                } else {
                    nonStatics.add(el);
                }
            } else {
                throw new IllegalArgumentException(el.getClass().getSimpleName() + "is not one of: Method, Field");
            }
        }

        return new Pair<List<T>, List<T>>(statics, nonStatics);
    }

}
