package com.offbytwo.iclojure;

import clojure.lang.LazySeq;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import com.offbytwo.iclojure.exceptions.StopInputException;
import com.offbytwo.iclojure.shortcuts.DescribeJavaObjectHandler;
import com.offbytwo.iclojure.signals.ControlCSignalHandler;
import com.offbytwo.iclojure.signals.RestoreTerminalHook;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static java.util.Arrays.asList;


public class Main {
    private ConsoleReader reader;
    private int inputNumber;
    private String namespace;
    private Var ns;
    private Var eval;
    final private Var completions;

    private StringBuffer inputSoFar = new StringBuffer();
    private DescribeJavaObjectHandler describeHandler;

    public Main(ConsoleReader reader) throws ClassNotFoundException, IOException {
        this.reader = reader;
        this.inputNumber = 0;
        this.namespace = "user";

        describeHandler = new DescribeJavaObjectHandler(reader);

        this.ns = RT.var("clojure.core", "*ns*");
        this.eval = RT.var("clojure.core", "eval");

        Var.pushThreadBindings(RT.map(ns, ns.deref()));
        RT.var("clojure.core", "in-ns").invoke(Symbol.create(null, "user"));

        Var use = RT.var("clojure.core", "use");

        use.invoke(RT.readString("[clojure.repl :only (source apropos dir pst doc find-doc)]"));
        use.invoke(RT.readString("[clojure.java.javadoc :only (javadoc)]"));
        use.invoke(RT.readString("[clojure.pprint :only (pprint)]"));

        RT.load("complete");
        this.completions = RT.var("complete", "completions");

        reader.addCompleter(new Completer() {
            public int complete(String buffer, int cursor, List<CharSequence> candidates) {
                String symbolToComplete;
                int matchStart;

                if (cursor <= buffer.length()) {
                    buffer = buffer.substring(0, cursor);
                }

                if (buffer.startsWith("(. ")) {
                    String prefix;
                    if (buffer.lastIndexOf(' ') == cursor - 1) {
                        prefix = "";
                        matchStart = cursor;
                    } else {
                        prefix = buffer.substring(buffer.lastIndexOf(' ') + 1);
                        matchStart = buffer.lastIndexOf(' ') + 1;
                    }

                    String form = buffer.replaceFirst("\\(\\. ", "").trim();
                    Object output = eval.invoke(RT.readString(form));
                    for (Method m : output.getClass().getMethods()) {
                        if (m.getName().startsWith(prefix)) {
                            candidates.add(m.getName());
                        }
                    }
                    return matchStart;
                } else if (buffer.startsWith("(")) {
                    symbolToComplete = buffer.replaceFirst("\\(", "");
                    matchStart = 1;
                } else {
                    symbolToComplete = buffer;
                    matchStart = 0;
                }

                LazySeq results = (LazySeq) completions.invoke(symbolToComplete);
                for (Object result : asList(results.toArray())) {
                    candidates.add((String) result);
                }
                return matchStart;
            }
        });
    }

    public void abortCurrentRead() throws IOException {
        this.reader.setCursorPosition(0);
        this.reader.killLine();

        if (this.inputSoFar.length() > 0) {
            this.inputSoFar = new StringBuffer();
            this.reader.println();
            this.reader.setPrompt(String.format("%s[%d]: ", namespace, inputNumber));
            this.reader.redrawLine();
            this.reader.flush();
        }
    }

    private Object read() throws IOException, StopInputException {
        String line;

        if (inputSoFar.length() > 0) {
            line = reader.readLine("... ");
        } else {
            line = reader.readLine(String.format("%s[%d]: ", namespace, inputNumber));
        }

        if (line.equals("exit")) {
            RT.var("clojure.core", "shutdown-agents").invoke();
            throw new StopInputException();
        } else if (line.startsWith("%")) {
            if (line.startsWith("%d")) {
                Object input = RT.readString(line.replace("%d", "").trim());
                describeHandler.describe(eval.invoke(input));
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
        } else if (line.trim().equals("") ) {
            return null;
        } else {
            try {
                Object read = RT.readString(inputSoFar.toString() + line);
                inputSoFar = new StringBuffer();
                return read;
            } catch (RuntimeException re) {
                if (re.getMessage().startsWith("EOF while reading")) {
                    inputSoFar.append(line + " ");
                    return null;
                }
            }
        }

        return null;
    }

    private String eval(Object line) throws StopInputException {
        if (line == null) {
            return null;
        }

        try {
            Object ret = eval.invoke(line);
            return RT.printString(ret);
        } catch (RuntimeException re) {
            // TODO print smarter stack traces here
            re.printStackTrace();
            return null;
        }
    }

    private void print(String output) throws IOException {
        if (output != null) {
            reader.println(String.format("%s[%d]= %s", namespace, inputNumber, output));
        }
        reader.println();
    }

    public void loop() {
        Object input;
        String output;

        try {
            while (true) {
                inputNumber += 1;
                input = read();
                if (input != null) {
                    output = eval(input);
                    print(output);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (StopInputException e) {
            return;
        }
    }


    public static void main(String[] args) {

        try {
            final ConsoleReader reader = new ConsoleReader();
            new RestoreTerminalHook(reader).install();
            final Main main = new Main(reader);
            new ControlCSignalHandler(main).install();

            main.preamble();
            main.loop();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to create console reader");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void preamble() throws IOException {
        reader.println("Clojure 1.3.0");
        reader.println();
        reader.println("IClojure 1.0 -- an enhanced Interactive Clojure");
        reader.println("?         -> Introduction and overview of IClojure's features");
        reader.println("?symbol   -> Print documentation for symbol");
        reader.println("??symbol  -> Show source of function or macro");
        reader.println("%d symbol -> Describe Java class (show constructors, methods and fields)");
        reader.println();
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


}
