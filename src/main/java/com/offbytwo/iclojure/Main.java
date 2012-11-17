package com.offbytwo.iclojure;

import clojure.lang.Compiler;
import clojure.lang.RT;
import com.offbytwo.iclojure.repl.IClojureRepl;
import com.offbytwo.iclojure.signals.ControlCSignalHandler;
import com.offbytwo.iclojure.signals.RestoreTerminalHook;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import org.apache.log4j.*;

import java.io.File;
import java.io.IOException;

import static clojure.lang.RT.var;


public class Main {

    public static Level LOG_LEVEL = Level.INFO;

    public static void usage() {
        System.out.println("Usage: java -cp iclojure.jar [com.offbytwo.iclojure.Main] [-i file]\n" +
                "\n" +
                "  With no options or args, runs an interactive Read-Eval-Print Loop\n" +
                "\n" +
                "    -i, --init path     Load a file or resource");
    }

    public static String getFileToLoad(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String currentArg = args[i];
            if (currentArg.equals("-i")) {
                if (i < args.length - 1) {
                    return args[i + 1];
                } else {
                    usage();
                    System.exit(1);
                }
            } else if (currentArg.equals("-h") || currentArg.equals("--help")) {
                usage();
                System.exit(0);
            }
        }

        return null;
    }

    public static void main(String[] args) {
        configureLogging();
        try {
            final ConsoleReader reader = new ConsoleReader();

            reader.setExpandEvents(false);
            configureHistory(reader);

            new RestoreTerminalHook(reader).install();
            final IClojureRepl repl = new IClojureRepl(reader);
            new ControlCSignalHandler(repl).install();

            preamble(reader);
            loadRequestedScripts(args);
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

    private static void configureLogging() {
        String homeFolder = System.getProperty("user.home");
        File logFile = new File(homeFolder, ".iclojure.log");

        Logger logger = Logger.getLogger("com.offbytwo.iclojure");
        logger.setLevel(LOG_LEVEL);

        try {
            logger.addAppender(new FileAppender(new SimpleLayout(), logFile.getAbsolutePath()));
        } catch (IOException e) {
            System.err.println(String.format("Unable to open log file %s. Logging to console", logFile));
            e.printStackTrace();
            logger.addAppender(new ConsoleAppender());
        }

        logger.info("Started iClojure REPL");
    }

    private static void loadRequestedScripts(String[] args) {
        String fileToLoad = getFileToLoad(args);
        if (fileToLoad != null && fileToLoad.length() > 1) {
            if (fileToLoad.startsWith("@")) {
                int offest = fileToLoad.startsWith("@/") ? 2 : 1;
                try {
                    RT.loadResourceScript(fileToLoad.substring(offest));
                } catch (IOException e) {
                    System.out.println("Error loading resource script: " + fileToLoad);
                }

            } else {
                try {
                    Compiler.loadFile(fileToLoad);
                } catch (IOException e) {
                    System.out.println("Error loading file " + fileToLoad);
                }
            }
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
        reader.println("???symbol        -> Show documentation from clojuredocs.org");
        reader.println("%d symbol        -> Describe Java class (show constructors, methods and fields)");
        reader.println("%f class         -> find all classes matching this name (supports globs)");
        reader.println("%f class package -> like the above, but restrict search to the given package");
        reader.println();
    }

}
