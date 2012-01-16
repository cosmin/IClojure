package com.offbytwo.iclojure.signals;

import jline.console.ConsoleReader;

public class RestoreTerminalHook extends Thread {
    public RestoreTerminalHook(final ConsoleReader reader) {
        super(new Runnable() {
            public void run() {
                try {
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
