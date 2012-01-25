package com.offbytwo.iclojure.completion;

import clojure.lang.LazySeq;
import clojure.lang.RT;
import clojure.lang.Var;
import jline.console.completer.Completer;
import jline.internal.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static java.util.Arrays.asList;

public class DefaultCompleter implements Completer {
    private final ClojureCompletionWrapper completions;

    public DefaultCompleter(ClojureCompletionWrapper wrapper) {
        this.completions = wrapper;
    }

    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        candidates.addAll(completions.getCompletionsForString(buffer));
        return 0;
    }
}
