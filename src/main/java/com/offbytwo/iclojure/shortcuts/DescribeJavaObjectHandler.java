package com.offbytwo.iclojure.shortcuts;

import jline.console.ConsoleReader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static com.offbytwo.iclojure.util.ReflectionUtils.formatParameterTypes;
import static com.offbytwo.iclojure.util.ReflectionUtils.sortStaticsFirst;
import static java.util.Arrays.asList;

public class DescribeJavaObjectHandler {
    private ConsoleReader reader;

    public DescribeJavaObjectHandler(ConsoleReader reader) {
        this.reader = reader;
    }

    public void describe(Object object) throws IOException {
        Class inspect;
        if (object instanceof Class) {
            inspect = (Class) object;
        } else {
            inspect = object.getClass();
        }

        List<Field> fields = asList(inspect.getFields());
        List<Method> methods = asList(inspect.getMethods());
        List<Constructor> constructors = asList(inspect.getConstructors());

        reader.println("Class: " + inspect.getCanonicalName());
        reader.println();
        for (Constructor c : constructors) {
            printConstructor(c);
        }
        reader.println();
        for (Method m : sortStaticsFirst(methods)) {
            printMethod(m);
        }
        reader.println();
        for (Field f : sortStaticsFirst(fields)) {
            printField(f);
        }
    }


    private void printConstructor(Constructor c) throws IOException {
        reader.println("<init> " + c.getName() + "(" + formatParameterTypes(c.getParameterTypes()) + ")");
    }

    private void printField(Field f) throws IOException {
        StringBuffer output = new StringBuffer();
        if (Modifier.isStatic(f.getModifiers())) {
            output.append("static ");
        }
        output.append(f.getType().getSimpleName());
        output.append(" ");
        output.append(f.getName());
        reader.println(output.toString());
    }

    private void printMethod(Method m) throws IOException {
        StringBuffer output = new StringBuffer();
        if (Modifier.isStatic(m.getModifiers())) {
            output.append("static ");
        }
        output.append(m.getReturnType().getSimpleName());
        output.append(" ");
        output.append(m.getName());
        output.append("(");
        output.append(formatParameterTypes(m.getParameterTypes()));
        output.append(")");
        reader.println(output.toString());
    }

}
