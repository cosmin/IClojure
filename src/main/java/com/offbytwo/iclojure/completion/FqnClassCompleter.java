package com.offbytwo.iclojure.completion;

import com.offbytwo.class_finder.ClassFinder;
import jline.console.completer.Completer;

import java.util.List;

public class FqnClassCompleter implements Completer {
    private ClassFinder classFinder;

    public FqnClassCompleter(ClassFinder classFinder) {
        this.classFinder = classFinder;
    }

    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        candidates.addAll(classFinder.findFQClassesStartingWith(buffer));
        return 0;
    }
}
