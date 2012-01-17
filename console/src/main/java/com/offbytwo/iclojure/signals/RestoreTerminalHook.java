package com.offbytwo.iclojure.signals;

import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

public class RestoreTerminalHook extends Thread {
    public RestoreTerminalHook(final ConsoleReader reader) {
        super(new Runnable() {
            public void run() {
                try {
                    if (reader.getHistory() instanceof FileHistory) {
                        ((FileHistory) reader.getHistory()).flush();
                    }
                    reader.getTerminal().restore();
                } catch (Exception e) {
                    // do nothing
                }
            }
        });
    }

    public void install() {
        Runtime.getRuntime().addShutdownHook(this);
    }
}
