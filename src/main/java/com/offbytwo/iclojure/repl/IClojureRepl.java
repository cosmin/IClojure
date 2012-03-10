package com.offbytwo.iclojure.repl;

import clojure.lang.Compiler;
import clojure.lang.*;
import com.offbytwo.class_finder.ClassFinder;
import com.offbytwo.iclojure.InputOutputCache;
import com.offbytwo.iclojure.completion.DelegatingCompleter;
import com.offbytwo.iclojure.exceptions.StopInputException;
import com.offbytwo.iclojure.handlers.DescribeJavaObjectHandler;
import com.offbytwo.iclojure.util.ConsoleOutputStreamWriter;
import jline.console.ConsoleReader;
import org.fusesource.jansi.AnsiString;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static clojure.lang.RT.*;
import static com.offbytwo.iclojure.InputOutput.ColoredText.*;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.RED;


public class IClojureRepl {
    private ConsoleReader reader;
    private int inputNumber;
    private Var ns = var("clojure.core", "*ns*");

    private DescribeJavaObjectHandler describeHandler;
    private Var output1 = var("clojure.core", "*1");
    private Var output2 = var("clojure.core", "*2");
    private Var output3 = var("clojure.core", "*3");
    private Var lastError = var("clojure.core", "*e");
    private Var pst;
    private OutputStreamWriter writer;
    private InputOutputCache ioCache = new InputOutputCache(1000);
    private ClassFinder classFinder = new ClassFinder();
    private StringBuilder inputSoFar;
    private String lastPrompt;
    private boolean clojure1_2;


    public IClojureRepl(final ConsoleReader reader) throws ClassNotFoundException, IOException {
        String clojureVersion = (String) var("clojure.core", "clojure-version").invoke();
        this.clojure1_2 = clojureVersion.startsWith("1.2");

        this.reader = reader;
        this.inputNumber = 0;

        this.writer = new ConsoleOutputStreamWriter(reader);
        this.describeHandler = new DescribeJavaObjectHandler(reader);

        createNecessaryThreadBindings();
        createUserNamespace();

        this.pst = var("clj-stacktrace.repl", "pst");

        reader.addCompleter(new DelegatingCompleter(classFinder));
        inputSoFar = new StringBuilder();
    }

    private Object read() throws IOException, StopInputException {
        String line = readLine(true);

        if (line == null || line.equals("exit")) {
            throw new StopInputException();
        } else if (line.startsWith("%")) {
            return handleShortcuts(line);
        } else if (line.startsWith("?")) {
            return handleHelp(line);
        } else if (line.trim().equals("")) {
            return null;
        } else {
            return readPotentiallyMultilineForm(line);
        }
    }

    private Object readPotentiallyMultilineForm(String line) {
        inputSoFar.append(line).append(" ");

        try {
            for (; ; ) {
                try {
                    if (inputSoFar.toString().trim().length() > 0) {
                        Object input = readString(inputSoFar.toString());
                        return input;
                    } else {
                        return null;
                    }
                } catch (Throwable t) {
                    if (t.getMessage().contains("EOF while reading")) {
                        try {
                            String newLine = readLine(false);
                            inputSoFar.append(newLine).append(" ");
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    } else {
                        printStackTrace(t);
                        return null;
                    }
                }
            }
        } finally {
            inputSoFar.setLength(0);
        }
    }

    private Object eval(Object line) throws StopInputException, IOException {
        try {
            Object ret = Compiler.eval(line);

            writer.flush();
            reader.getOutput().flush();

            return ret;
        } catch (IOException ie){
            throw ie;
        } catch (Throwable t) {
            lastError.set(t);
            printStackTrace(t);
            return null;
        }
    }


    private void print(Object output) throws IOException {
        try{
            if (output != null) {
                reader.print(getOutputPrompt());
                RT.printString(output);
                reader.println(RT.printString(output));
            }
            reader.println();
        } catch (IOException ie){
            throw ie;
        } catch (Throwable t){
            lastError.set(t);
            printStackTrace(t);
        }
        
    }

    public void loop() {
        Object input;
        Object output;

        try {
            while (true) {
                inputNumber += 1;
                input = read();
                if (input == null) {
                    cacheInputOutput(null, null);
                    continue;
                }
                output = eval(input);
                cacheInputOutput(input, output);
                print(output);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (StopInputException e) {
            //
        }
    }

    private void cacheInputOutput(Object input, Object output) {
        captureLast3Outputs(output);
        ioCache.add(input, output);
    }

    private void createUserNamespace() throws ClassNotFoundException, IOException {
        Namespace userNs = Namespace.findOrCreate(Symbol.create(null, "user"));

        RT.load("clojure/repl");
        List<String> replFns = new ArrayList<String>(Arrays.asList("source", "apropos", "dir"));
        if (!clojure1_2) {
            replFns.add("doc");
            replFns.add("find-doc");
        }

        for (String name : replFns) {
            userNs.refer(Symbol.create(null, name), var("clojure.repl", name));
        }

        if (!clojure1_2) {
            RT.load("clojure/java/javadoc");
            userNs.refer(Symbol.create(null, "javadoc"), var("clojure.java.javadoc", "javadoc"));
        }

        RT.load("clojure/pprint");
        userNs.refer(Symbol.create(null, "pprint"), var("clojure.pprint", "pprint"));

        RT.load("clj_stacktrace/repl");
        userNs.refer(Symbol.create(null, "pst"), var("clj-stacktrace.repl", "pst"));

        this.ns.set(userNs);
    }

    private void createNecessaryThreadBindings() {

        var("user", "input").bindRoot(ioCache.getInputLookupFn());
        var("user", "output").bindRoot(ioCache.getOutputLookupFn());

        Var.pushThreadBindings(map(
                var("clojure.core", "*out*"), writer,
                ns, ns.deref(),
                output1, null,
                output2, null,
                output3, null,
                lastError, null
        ));
    }

    public void abortCurrentRead() throws IOException {
        this.reader.println();
        this.reader.println("Keyboard Interrupt");
        this.reader.println();

        this.inputSoFar.setLength(0);

        this.reader.setCursorPosition(0);
        this.reader.killLine();

        this.reader.setPrompt(lastPrompt);

        this.reader.redrawLine();
        this.reader.flush();

    }

    private Object handleHelp(String line) throws IOException {
        String expanded;

        if (line.equals("?")) {
            help();
            return null;
        } else if (line.startsWith("??")) {
            expanded = "(clojure.repl/source " + line.replace("??", "") + ")";
        } else {
            if (clojure1_2) {
                expanded = "(clojure.core/doc " + line.replace("?", "") + ")";
            } else {
                expanded = "(clojure.repl/doc " + line.replace("?", "") + ")";
            }
        }

        try {
            return readString(expanded);
        } catch (Throwable t) {
            reader.println("Error reading: " + expanded);
            return null;
        }
    }

    private Object handleShortcuts(String line) throws IOException {
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
                input = readString(line.replace("%d", "").trim());
            } catch (Throwable t) {
                reader.println("Error: unable to read form after %d");
                return null;
            }

            Object output;
            try {
                output = Compiler.eval(input);
            } catch (Throwable t) {
                t.printStackTrace();
                reader.println("Error: unable to evaluate form after %d");
                return null;
            }
            describeHandler.describe(output);
        } else if (line.startsWith("%f")) {
            String searchFor = line.replace("%f", "").trim();
            String className = "";
            String packageName = "";

            if (searchFor.contains(" ")) {
                String[] components = searchFor.split(" ");
                className = components[0];
                packageName = components[1];
            } else {
                className = searchFor;
            }

            Set<String> matches;

            if (searchFor.contains("*") || searchFor.contains("?")) {
                matches = classFinder.findClassesInPackageByGlob(packageName, className);
            } else {
                matches = classFinder.findClassesInPackageByName(packageName, className);
            }

            reader.printColumns(matches);

        } else {
            reader.println("Unknown command!");
        }

        return null;
    }

    private String readLine(boolean firstLine) throws IOException {
        String line = null;

        int promptLength = new AnsiString(getPrompt()).length();

        StringBuilder morePrompt = new StringBuilder();
        for (int i = 0; i < promptLength - 5; i++) {
            morePrompt.append(" ");
        }
        morePrompt.append(color(BLUE, "...: "));
        morePrompt.append(revertToDefaultColor());


        while (line == null) {
            if (firstLine) {
                line = reader.readLine(getPrompt());
            } else {
                line = reader.readLine(morePrompt.toString());
            }

            if (line == null) {
                System.out.print("\nDo you really want to exit ([y]/n)? ");
                char input = (char) reader.readCharacter('y', 'n', (char) 10);

                if (input != 'n') {
                    return null;
                } else {
                    System.out.println();
                    System.out.println();
                    reader.flush();
                }
            }
        }
        return line;
    }

    private String getPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append(color(BLUE, format("%s[", ns.deref())));
        sb.append(colorBright(BLUE, valueOf(inputNumber)));
        sb.append(color(BLUE, "]: "));
        sb.append(revertToDefaultColor());
        lastPrompt = sb.toString();
        return lastPrompt;
    }

    private String getOutputPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append(color(RED, format("%s[", ns.deref())));
        sb.append(colorBright(RED, valueOf(inputNumber)));
        sb.append(color(RED, "]= "));
        sb.append(revertToDefaultColor());
        return sb.toString();
    }

    private void captureLast3Outputs(Object ret) {
        output3.set(output2.deref());
        output2.set(output1.deref());
        output1.set(ret);
    }

    private void printStackTrace(Throwable t) {
        try {
            reader.println(color(RED, "---------------------------------------------------------------------------"));
            reader.print(revertToDefaultColor());
            reader.flush();
            pst.invoke(t);
            reader.println();
            StringBuffer sb = new StringBuffer();
            sb.append(color(RED, t.getClass().getSimpleName()));
            sb.append(revertToDefaultColor());
            sb.append(": ");
            sb.append(t.getLocalizedMessage());
            reader.println(sb.toString());
            reader.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        reader.println();
        reader.println("(input i)   => return the input from line i");
        reader.println("(output i)  => return the output from line i");
    }


}
