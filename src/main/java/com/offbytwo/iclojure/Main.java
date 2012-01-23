package com.offbytwo.iclojure;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import com.offbytwo.iclojure.exceptions.StopInputException;
import com.offbytwo.iclojure.shortcuts.DescribeJavaObjectHandler;
import com.offbytwo.iclojure.signals.ControlCSignalHandler;
import com.offbytwo.iclojure.signals.RestoreTerminalHook;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static com.offbytwo.iclojure.ColoredText.*;
import static org.fusesource.jansi.Ansi.Color.*;


public class Main {
    private ConsoleReader reader;
    private int inputNumber;
    private String namespace;
    private Var ns;
    private Var eval = RT.var("clojure.core", "eval");

    private StringBuffer inputSoFar = new StringBuffer();
    private DescribeJavaObjectHandler describeHandler;
    private Var output1;
    private Var output2;
    private Var output3;
    private Var lastError;
    private OutputStreamWriter writer;

    public Main(final ConsoleReader reader) throws ClassNotFoundException, IOException {
        this.reader = reader;
        this.inputNumber = 0;
        this.namespace = "user";

        this.output1 = RT.var("clojure.core", "*1");
        this.output2 = RT.var("clojure.core", "*2");
        this.output3 = RT.var("clojure.core", "*3");
        this.lastError = RT.var("clojure.core", "*e");

        describeHandler = new DescribeJavaObjectHandler(reader);

        this.ns = RT.var("clojure.core", "*ns*");

        Var out = RT.var("clojure.core", "*out*");

        writer = new OutputStreamWriter(new OutputStream() {
            @Override
            public void write(int i) throws IOException {
                reader.print(String.valueOf((char) i));
            }
        });
        Var.pushThreadBindings(RT.map(out, writer));


        Var.pushThreadBindings(RT.map(ns, ns.deref()));
        Var.pushThreadBindings(RT.map(output1, null));
        Var.pushThreadBindings(RT.map(output2, null));
        Var.pushThreadBindings(RT.map(output3, null));
        Var.pushThreadBindings(RT.map(lastError, null));

        RT.var("clojure.core", "in-ns").invoke(Symbol.create(null, "user"));

        Var use = RT.var("clojure.core", "use");

        use.invoke(RT.readString("[clojure.repl :only (source apropos dir pst doc find-doc)]"));
        use.invoke(RT.readString("[clojure.java.javadoc :only (javadoc)]"));
        use.invoke(RT.readString("[clojure.pprint :only (pprint)]"));


        reader.addCompleter(new ClojureCompleter());
    }

    public void abortCurrentRead() throws IOException {
        this.reader.setCursorPosition(0);
        this.reader.killLine();

        if (this.inputSoFar.length() > 0) {
            this.inputSoFar = new StringBuffer();
            this.reader.println();
            this.reader.setPrompt(getPrompt());
            this.reader.redrawLine();
            this.reader.flush();
        }
    }

    private Object read() throws IOException, StopInputException {
        String line = null;

        while( line == null) {
            if (inputSoFar.length() > 0) {
                line = reader.readLine("... ");
            } else {
                line = reader.readLine(getPrompt());
            }

            if (line == null) {
                System.out.print("\nDo you really want to exit ([y]/n)? ");
                char input = (char) reader.readCharacter('y', 'n', (char) 10);

                if (input != 'n') {
                    System.exit(1);
                } else {
                    System.out.println();
                    System.out.println();
                    reader.flush();
                }
            }
        }

        if (line.equals("exit")) {
            RT.var("clojure.core", "shutdown-agents").invoke();
            throw new StopInputException();
        } else if (line.startsWith("%")) {
            if (line.startsWith("%d")) {
                if (line.replace("%d", "").trim().length() == 0) {
                    Object output = output1.deref();
                    if (output instanceof Var) {
                        describeHandler.describe(((Var) output).deref());
                    } else {
                        describeHandler.describe(output);
                    }
                    return null;
                }

                Object input;
                try {
                    input = RT.readString(line.replace("%d", "").trim());
                } catch (RuntimeException re) {
                    reader.println("Error: unable to read form after %d");
                    return null;
                }
                Object output;

                try {
                    output = eval.invoke(input);
                } catch (Exception e) {
                    e.printStackTrace();
                    reader.println("Error: unable to evaluate form after %d");
                    return null;
                }
                describeHandler.describe(output);
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
        } else if (line.trim().equals("")) {
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

    private String getPrompt() {
        StringBuffer sb = new StringBuffer();
        sb.append(color(BLUE, String.format("%s[", namespace)));
        sb.append(colorBright(BLUE, String.valueOf(inputNumber)));
        sb.append(color(BLUE, "]: "));
        sb.append(revertToDefaultColor());
        return sb.toString();
    }

    private String getOutputPrompt() {
        StringBuffer sb = new StringBuffer();
        sb.append(color(RED, String.format("%s[", namespace)));
        sb.append(colorBright(RED, String.valueOf(inputNumber)));
        sb.append(color(RED, "]= "));
        sb.append(revertToDefaultColor());
        return sb.toString();
    }


    private String eval(Object line) throws StopInputException, IOException {
        if (line == null) {
            return null;
        }

        try {
            Object ret = eval.invoke(line);

            writer.flush();
            reader.getOutput().flush();

            output3.set(output2.deref());
            output2.set(output1.deref());
            output1.set(ret);
            return RT.printString(ret);
        } catch (RuntimeException re) {
            // TODO print smarter stack traces here
            re.printStackTrace();
            lastError.set(re);
            return null;
        }
    }

    private void print(String output) throws IOException {
        if (output != null) {
            reader.print(getOutputPrompt());
            reader.println(output);
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

            configureHistory(reader);

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

    private static void configureHistory(ConsoleReader reader) throws IOException {
        String homeFolder = System.getProperty("user.home");
        File historyFile = new File(homeFolder, ".iclojure_history");
        reader.setHistory(new FileHistory(historyFile));
        reader.setHistoryEnabled(true);
    }

    public void preamble() throws IOException {
        String clojureVersion = (String) RT.var("clojure.core", "clojure-version").invoke();

        reader.println("Clojure " + clojureVersion);
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
