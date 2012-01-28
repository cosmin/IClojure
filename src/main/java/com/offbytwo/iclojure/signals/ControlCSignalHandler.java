package com.offbytwo.iclojure.signals;

import com.offbytwo.iclojure.repl.IClojureRepl;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.IOException;

public class ControlCSignalHandler implements SignalHandler {

    private IClojureRepl repl;

    public ControlCSignalHandler(IClojureRepl repl) {
        this.repl = repl;
    }

    public void install() {
        Signal signal = new Signal("INT");
        Signal.handle(signal, this);
    }

    public void handle(Signal signal) {
        try {
            repl.abortCurrentRead();
        } catch (IOException e) {
            //
        }
    }
}
