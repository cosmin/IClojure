import clojure.lang.*;
import jline.console.ConsoleReader;

import java.io.IOException;


public class Main {
    private ConsoleReader reader;
    private int inputNumber;
    private String namespace;
    private Var ns;

    public Main(ConsoleReader reader) {
        this.reader = reader;
        this.inputNumber = 0;
        this.namespace = "user";

        this.ns = RT.var("clojure.core", "*ns*");
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
        Var eval = RT.var("clojure.core", "eval");
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
        }
        return RT.readString(line);
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
