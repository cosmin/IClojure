package com.offbytwo.iclojure.signals;

import com.offbytwo.iclojure.Main;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.IOException;

public class ControlCSignalHandler implements SignalHandler {

    private Main main;

    public ControlCSignalHandler(Main main) {

        this.main = main;
    }

    public void install() {
        Signal signal = new Signal("INT");
        Signal.handle(signal, this);
    }

    public void handle(Signal signal) {
        try {
            main.abortCurrentRead();
        } catch (IOException e) {
            //
        }
    }
}
