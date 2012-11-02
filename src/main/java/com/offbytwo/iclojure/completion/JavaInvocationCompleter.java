package com.offbytwo.iclojure.completion;

import clojure.lang.RT;
import clojure.lang.Var;
import jline.console.completer.Completer;

import java.lang.reflect.Method;
import java.util.List;

public class JavaInvocationCompleter implements Completer {
    private ClojureCompletionWrapper completions;
    private Var eval;

    public JavaInvocationCompleter(ClojureCompletionWrapper wrapper) {
        completions = wrapper;
        this.eval = RT.var("clojure.core", "eval");
    }

    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        int matchStart = 0;

        if (buffer.lastIndexOf(' ') > 0) {
            // there is a previous form
            String prefix;
            if (buffer.lastIndexOf(' ') == cursor - 1) {
                prefix = "";
                matchStart = cursor;
            } else {
                prefix = buffer.substring(buffer.lastIndexOf(' ') + 1);
                matchStart += buffer.lastIndexOf(' ') + 1;
            }

            String form = buffer.replaceFirst("\\(\\. ", "").trim();
	    try {
		Object output = eval.invoke(RT.readString(form));
		for (Method m : output.getClass().getMethods()) {
		    if (m.getName().startsWith(prefix)) {
			candidates.add(m.getName());
		    }
		}
		return matchStart;
	    } catch (RuntimeException re) {
		return 0;
	    }
        }

        return matchStart;
    }
}
