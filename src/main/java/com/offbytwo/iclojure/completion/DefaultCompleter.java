package com.offbytwo.iclojure.completion;

import com.iclojure.jline.console.completer.Completer;

import java.util.List;

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
