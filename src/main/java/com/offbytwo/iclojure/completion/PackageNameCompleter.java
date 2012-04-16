package com.offbytwo.iclojure.completion;

import com.offbytwo.class_finder.ClassFinder;
import com.iclojure.jline.console.completer.Completer;

import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;

public class PackageNameCompleter implements Completer {
    private ClassFinder classFinder;

    public PackageNameCompleter(ClassFinder classFinder) {
        this.classFinder = classFinder;
    }

    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        if(buffer.contains(" ")) {
            int matchStart = buffer.lastIndexOf(" ") + 1;
            String classNamePrefix = buffer.substring(matchStart);
            String packageName = buffer.substring(0, buffer.indexOf(" "));

            NavigableMap<String,NavigableSet<String>> classesByPackageName = classFinder.getClassesByPackageName();
            if (classesByPackageName.containsKey(packageName)) {
                candidates.addAll(classesByPackageName.get(packageName).subSet(classNamePrefix, classNamePrefix + "\uffff"));
            }
            return matchStart;
        } else {
            candidates.addAll(classFinder.findPackagesStartingWith(buffer));
            return 0;
        }
    }
}
