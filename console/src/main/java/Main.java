import clojure.lang.*;
import clojure.lang.Compiler;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Main {
    private ConsoleReader reader;
    private int inputNumber;
    private String namespace;
    private Var ns;
    private Var eval;

    public String join(String separator, Object... members) {
        return RT.var("clojure.string", "join").invoke(separator, members).toString();
    }

    public String formatParameterTypes(Class... parameterTypes) {
        List<String> classNames = new ArrayList<String>();

        for (Class c : parameterTypes) {
            classNames.add(c.getSimpleName());
        }

        return join(", ", classNames.toArray());
    }

    public <T> List<T> filterByStatic(List<T> all, boolean wantStatic) {
        List<T> ret = new ArrayList<T>();

        for (T el : all) {
            if (el instanceof Method) {
                if (wantStatic == Modifier.isStatic(((Method) el).getModifiers())) {
                    ret.add(el);
                }
            } else if (el instanceof Field) {
                if (wantStatic == Modifier.isStatic(((Field) el).getModifiers())) {
                    ret.add(el);
                }
            }
        }

        return ret;
    }


    public void describe(Object object) throws IOException {
        Class inspect;
        if (object instanceof Class) {
            inspect = (Class) object;
        } else {
            inspect = object.getClass();
        }

        List<Field> fields = Arrays.asList(inspect.getFields());
        List<Method> methods = Arrays.asList(inspect.getMethods());
        List<Constructor> constructors = Arrays.asList(inspect.getConstructors());

        reader.println("Class: " + inspect.getCanonicalName());

        reader.println();


        for (Constructor c : constructors) {
            reader.println("<init> " + c.getName() + "(" + formatParameterTypes(c.getParameterTypes()) + ")");
        }

        reader.println();

        for (Method m : filterByStatic(methods, true)) {
            printMethod(m);
        }
        for (Method m : filterByStatic(methods, false)) {
            printMethod(m);
        }

        reader.println();

        for (Field f : filterByStatic(fields, true)) {
            printField(f);
        }
        for (Field f : filterByStatic(fields, false)) {
            printField(f);
        }
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

    public Main(ConsoleReader reader) {
        this.reader = reader;
        this.inputNumber = 0;
        this.namespace = "user";

        this.ns = RT.var("clojure.core", "*ns*");
        this.eval = RT.var("clojure.core", "eval");

        Var.pushThreadBindings(RT.map(ns, ns.deref()));
        RT.var("clojure.core", "in-ns").invoke(Symbol.create(null, "user"));

        Var use = RT.var("clojure.core", "use");

        use.invoke(RT.readString("[clojure.repl :only (source apropos dir pst doc find-doc)]"));
        use.invoke(RT.readString("[clojure.java.javadoc :only (javadoc)]"));
        use.invoke(RT.readString("[clojure.pprint :only (pprint)]"));
    }

    public void help() throws IOException {
        reader.println("doc         => show documentation of the given function or macro");
        reader.println("source      => show source of the given function or macro");
        reader.println();
        reader.println("dir         => show all the names in the given namespace");
        reader.println("apropos     => all definitions in all loaded namespaces that match the given pattern");
        reader.println("find-doc    => print doc for any var whose doc or name matches the given pattern");
        reader.println();
        reader.println("pst         => print stack trace of the given exception");
        reader.println("pp          => pretty print the last value");
        reader.println("pprint      => pretty print the given value");
    }

    public void loop() {
        Object input;
        String output;

        try {
            while (true) {
                inputNumber += 1;
                input = read();
                output = eval(input);
                print(output);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (StopInputException e) {
            //
        }
    }

    private void print(String output) throws IOException {
        if (output != null) {
            reader.println(String.format("%s[%d]= %s", namespace, inputNumber, output));
        }
        reader.println();
    }

    private String eval(Object line) throws StopInputException {
        if (line == null) {
            return null;
        }

        try {
            Object ret = eval.invoke(line);
            return RT.printString(ret);
        } catch (RuntimeException re) {
            re.printStackTrace();
            return null;
        }
    }

    private Object read() throws IOException, StopInputException {
        String line = reader.readLine(String.format("%s[%d]: ", namespace, inputNumber));

        if (line.equals("exit")) {
            throw new StopInputException();
        } else if (line.startsWith("%")) {
            if (line.startsWith("%d")) {
                Object input = RT.readString(line.replace("%d", "").trim());
                describe(eval.invoke(input));
            } else {
                reader.println("Unknown command!");
            }
        } else if (line.startsWith("?")) {
            if (line.equals("?")) {
                help();
            } else if (line.startsWith("??")) {
                return RT.readString("(source " + line.replace("??", "") + ")");
            } else {
                return RT.readString("(doc " + line.replace("?", "") + ")");
            }
        } else {
            return RT.readString(line);
        }

        return null;
    }


    public static void main(String[] args) {

        try {
            final ConsoleReader reader = new ConsoleReader();

            // handle abnormal termination
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    try {
                        reader.getTerminal().restore();
                    } catch (Exception e) {

                    }
                }
            }));


            reader.println("Clojure 1.3.0");
            reader.println();
            reader.println("IClojure 1.0 -- an enhanced Interactive Clojure");
            reader.println("?         -> Introduction and overview of IClojure's features");
            reader.println("?symbol   -> Print documentation for symbol");
            reader.println("??symbol  -> Show source of function or macro");
            reader.println("%d symbol -> Describe Java class (show constructors, methods and fields)");
            reader.println();

            Main main = new Main(reader);
            main.loop();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to create console reader");
        }
    }

    private class StopInputException extends Throwable {
    }
}
