package com.offbytwo.iclojure.completion;

import jline.console.completer.Completer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DelegatingCompleter implements Completer {
    public Map<Pattern, Completer> completers = new LinkedHashMap<Pattern, Completer>();

    public static Pattern pattern(String matchThis) {
        return Pattern.compile(matchThis + "(.*)");
    }

    public DelegatingCompleter() throws IOException, ClassNotFoundException {
        ClojureCompletionWrapper wrapper = new ClojureCompletionWrapper();

        DefaultCompleter DEFAULT_COMPLETER = new DefaultCompleter(wrapper);
        JavaInvocationCompleter JAVA_COMPLETER = new JavaInvocationCompleter(wrapper);

        completers.put(pattern("\\(\\. "), JAVA_COMPLETER);
        completers.put(pattern("\\?\\?"), DEFAULT_COMPLETER);
        completers.put(pattern("\\?"), DEFAULT_COMPLETER);
        completers.put(pattern("%d "), DEFAULT_COMPLETER);
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
