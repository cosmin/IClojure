package com.offbytwo.iclojure.completion;

import com.offbytwo.class_finder.ClassFinder;
import com.iclojure.jline.console.completer.Completer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DelegatingCompleter implements Completer {
    public Map<Pattern, Completer> completers = new LinkedHashMap<Pattern, Completer>();
    private ClassFinder classFinder;

    public static Pattern pattern(String matchThis) {
        return Pattern.compile(matchThis + "(.*)");
    }

    public DelegatingCompleter(ClassFinder classFinder) throws IOException, ClassNotFoundException {
        this.classFinder = classFinder;
        ClojureCompletionWrapper wrapper = new ClojureCompletionWrapper();

        DefaultCompleter DEFAULT_COMPLETER = new DefaultCompleter(wrapper);
        JavaInvocationCompleter JAVA_COMPLETER = new JavaInvocationCompleter(wrapper);
        Completer FQN_CLASS_FINDER = new FqnClassCompleter(classFinder);

        // java interop
        completers.put(pattern("\\(\\. "), JAVA_COMPLETER);

        // import
        Completer PACKAGE_NAME_COMPLETER = new PackageNameCompleter(classFinder);
        completers.put(pattern("\\(import \\["), PACKAGE_NAME_COMPLETER);
        completers.put(pattern("\\(import '\\("), PACKAGE_NAME_COMPLETER);
        completers.put(pattern("\\(import '"), FQN_CLASS_FINDER);
        completers.put(pattern("\\(import "), FQN_CLASS_FINDER);


        // shorthands
        completers.put(pattern("\\?\\?"), DEFAULT_COMPLETER);
        completers.put(pattern("\\?"), DEFAULT_COMPLETER);
        completers.put(pattern("%d "), DEFAULT_COMPLETER);

        // regular
        completers.put(pattern("\\("), DEFAULT_COMPLETER);
        completers.put(pattern(""), DEFAULT_COMPLETER);
    }

    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        int matchStart = 0;

        if (buffer.startsWith(" ")) {
            String trimmed = buffer.trim();
            matchStart += buffer.indexOf(trimmed.charAt(0));
            buffer = buffer.substring(matchStart);
        }

        for (Pattern p : completers.keySet()) {
            Matcher matcher = p.matcher(buffer);
            if (matcher.matches()) {
                int groups = matcher.groupCount();
                String lastGroup = matcher.group(groups);

                return matchStart + matcher.start(groups) + completers.get(p).complete(lastGroup, cursor, candidates);
            }
        }

        return 0;
    }
}
