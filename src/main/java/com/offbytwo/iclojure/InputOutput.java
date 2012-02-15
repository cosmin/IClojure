package com.offbytwo.iclojure;

import org.fusesource.jansi.Ansi;

public class InputOutput {
    public static class ColoredText {

        public static String color(Ansi.Color color, String text) {
            return color(color, text, false);
        }

        public static String colorBright(Ansi.Color color, String text) {
            return color(color, text, true);
        }

        private static String color(Ansi.Color color, String text, boolean bright) {
            StringBuffer sb = new StringBuffer();

            Ansi colorSequence = getColorAnsiSequence(color, bright);

            sb.append(colorSequence).append(text);

            if (bright) {
                sb.append(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD_OFF));
            }

            return sb.toString();
        }

        public static String revertToDefaultColor() {
            return Ansi.ansi().fg(Ansi.Color.DEFAULT).toString();
        }

        private static Ansi getColorAnsiSequence(Ansi.Color color, boolean bright) {
            Ansi ansi = Ansi.ansi();
            if (bright) {
                ansi.a(Ansi.Attribute.INTENSITY_BOLD);
            }
            return ansi.fg(color);
        }
    }
}
