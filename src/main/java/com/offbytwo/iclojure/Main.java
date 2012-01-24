package com.offbytwo.iclojure;

import com.offbytwo.iclojure.repl.IClojureRepl;
import com.offbytwo.iclojure.signals.ControlCSignalHandler;
import com.offbytwo.iclojure.signals.RestoreTerminalHook;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

import java.io.File;
import java.io.IOException;

import static clojure.lang.RT.var;


public class Main {
    public static void main(String[] args) {

        try {
            final ConsoleReader reader = new ConsoleReader();

            reader.setExpandEvents(false);
            configureHistory(reader);

            new RestoreTerminalHook(reader).install();
            final IClojureRepl repl = new IClojureRepl(reader);
            new ControlCSignalHandler(repl).install();

            preamble(reader);
            repl.loop();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to create console reader");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            var("clojure.core", "shutdown-agents").invoke();
        }
    }

    private static void configureHistory(ConsoleReader reader) throws IOException {
        String homeFolder = System.getProperty("user.home");
        File historyFile = new File(homeFolder, ".iclojure_history");
        reader.setHistory(new FileHistory(historyFile));
        reader.setHistoryEnabled(true);
    }

    public static void preamble(ConsoleReader reader) throws IOException {
        String clojureVersion = (String) var("clojure.core", "clojure-version").invoke();

        String iClojureVersion = Main.class.getPackage().getImplementationVersion();
        if (iClojureVersion == null) {
            iClojureVersion = "DEV";
        }

        reader.println("Clojure " + clojureVersion);
        reader.println();
        reader.println("IClojure " + iClojureVersion + " -- an enhanced Interactive Clojure");
        reader.println("?                -> Introduction and overview of IClojure's features");
        reader.println("?symbol          -> Print documentation for symbol");
        reader.println("??symbol         -> Show source of function or macro");
        reader.println("%d symbol        -> Describe Java class (show constructors, methods and fields)");
        reader.println("%f class         -> find all classes matching this name (supports globs)");
        reader.println("%f class package -> like the above, but restrict search to the given package");
        reader.println();
    }

}
