package com.offbytwo.iclojure.repl;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import com.offbytwo.iclojure.InputOutputCache;
import com.offbytwo.iclojure.completion.DefaultCompleter;
import com.offbytwo.iclojure.completion.DelegatingCompleter;
import com.offbytwo.iclojure.exceptions.StopInputException;
import com.offbytwo.iclojure.handlers.DescribeJavaObjectHandler;
import com.offbytwo.iclojure.util.ClassFinder;
import com.offbytwo.iclojure.util.ConsoleOutputStreamWriter;
import jline.console.ConsoleReader;
import org.fusesource.jansi.AnsiString;

import java.io.IOException;
import java.io.OutputStreamWriter;
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
    private String namespace;
    private Var ns = var("clojure.core", "*ns*");
    private Var eval = var("clojure.core", "eval");

    private StringBuffer inputSoFar = new StringBuffer();
    private DescribeJavaObjectHandler describeHandler;
    private Var output1 = var("clojure.core", "*1");
    private Var output2 = var("clojure.core", "*2");
    private Var output3 = var("clojure.core", "*3");
    private Var lastError = var("clojure.core", "*e");
    private Var pst;
    private OutputStreamWriter writer;
    private InputOutputCache ioCache = new InputOutputCache(1000);
    private ClassFinder classFinder = new ClassFinder();


    public IClojureRepl(final ConsoleReader reader) throws ClassNotFoundException, IOException {
        this.reader = reader;
        this.inputNumber = 0;
        this.namespace = "user";

        this.writer = new ConsoleOutputStreamWriter(reader);
        this.describeHandler = new DescribeJavaObjectHandler(reader);

        createNecessaryThreadBindings();
        createUserNamespace();

        this.pst = var("clj-stacktrace.repl", "pst");

        reader.addCompleter(new DelegatingCompleter(classFinder));
    }

    private Object read() throws IOException, StopInputException {
        String line = readLine();

        if (line == null || line.equals("exit")) {
            throw new StopInputException();
        } else if (line.startsWith("%")) {
            return handleShortcuts(line);
        } else if (line.startsWith("?")) {
            return handleHelp(line);
        } else if (line.trim().equals("")) {
            return null;
        } else {
            try {
                Object read = readString(inputSoFar.toString() + line);
                inputSoFar = new StringBuffer();
                return read;
            } catch (RuntimeException re) {
                if (re.getMessage().startsWith("EOF while reading")) {
                    inputSoFar.append(line + " ");
                    return null;
                } else {
                    re.printStackTrace();
                    return null;
                }
            }
        }
    }

    private Object eval(Object line) throws StopInputException, IOException {
        try {
            Object ret = eval.invoke(line);

            writer.flush();
            reader.getOutput().flush();

            return ret;
        } catch (RuntimeException re) {
            lastError.set(re);
            printStackTrace(re);
            return null;
        }
    }


    private void print(Object output) throws IOException {
        if (output != null) {
            reader.print(getOutputPrompt());
            RT.printString(output);
            reader.println(RT.printString(output));
        }
        reader.println();
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

    private void createUserNamespace() {
        var("clojure.core", "in-ns").invoke(Symbol.create(null, "user"));
        Var use = var("clojure.core", "use");
        use.invoke(readString("[clojure.repl :only (source apropos dir doc find-doc)]"));
        use.invoke(readString("[clojure.java.javadoc :only (javadoc)]"));
        use.invoke(readString("[clojure.pprint :only (pprint)]"));
        use.invoke(readString("[clj-stacktrace.repl :only (pst)]"));
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

    private Object handleHelp(String line) throws IOException {
        if (line.equals("?")) {
            help();
            return null;
        } else if (line.startsWith("??")) {
            return readString("(clojure.repl/source " + line.replace("??", "") + ")");
        } else {
            return readString("(clojure.repl/doc " + line.replace("?", "") + ")");
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
                matches = classFinder.findClassesInPackageByName(packageName, className);
            } else {
                matches = classFinder.findClassesInPackageByGlob(packageName, className);
            }

            reader.printColumns(matches);

        } else {
            reader.println("Unknown command!");
        }

        return null;
    }

    private String readLine() throws IOException {
        String line = null;

        int promptLength = new AnsiString(getPrompt()).length();

        StringBuffer morePrompt = new StringBuffer();
        for (int i = 0; i < promptLength - 5; i++) {
            morePrompt.append(" ");
        }
        morePrompt.append(color(BLUE, "...: "));
        morePrompt.append(revertToDefaultColor());


        while (line == null) {
            if (inputSoFar.length() > 0) {
                line = reader.readLine(morePrompt.toString());
            } else {
                line = reader.readLine(getPrompt());
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
        StringBuffer sb = new StringBuffer();
        sb.append(color(BLUE, format("%s[", namespace)));
        sb.append(colorBright(BLUE, valueOf(inputNumber)));
        sb.append(color(BLUE, "]: "));
        sb.append(revertToDefaultColor());
        return sb.toString();
    }

    private String getOutputPrompt() {
        StringBuffer sb = new StringBuffer();
        sb.append(color(RED, format("%s[", namespace)));
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

    private void printStackTrace(RuntimeException re) throws IOException {
        reader.println(color(RED, "---------------------------------------------------------------------------"));
        reader.print(revertToDefaultColor());
        reader.flush();
        pst.invoke(re);
        reader.println();
        StringBuffer sb = new StringBuffer();
        sb.append(color(RED, re.getClass().getSimpleName()));
        sb.append(revertToDefaultColor());
        sb.append(": ");
        sb.append(re.getLocalizedMessage());
        reader.println(sb.toString());
        reader.flush();
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
