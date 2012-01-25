package com.offbytwo.iclojure.completion;

import clojure.lang.LazySeq;
import clojure.lang.RT;
import clojure.lang.Var;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class ClojureCompletionWrapper {
    private Var completions;
    private Var eval;

    public ClojureCompletionWrapper() {
        try {
            RT.load("complete");
        } catch (IOException e) {
            throw new RuntimeException("Error: Unable to load complete.clj", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error: Unable to load complete.clj", e);
        }
        this.eval = RT.var("clojure.core", "eval");
        this.completions = RT.var("complete", "completions");
    }

    public List<String> getCompletionsForString(String toComplete) {
        List<String> returnThis = new ArrayList<String>();

        LazySeq results = (LazySeq) completions.invoke(toComplete);
        for (Object result : asList(results.toArray())) {
            returnThis.add((String) result);
        }

        return returnThis;
    }
}
