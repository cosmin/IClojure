import jline.console.ConsoleReader;

import java.io.IOException;


public class Main {
    public static void reset(ConsoleReader reader) {
        try {
            reader.getTerminal().restore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ConsoleReader reader = new ConsoleReader();
            reader.println("Clojure 1.3.0");
            reader.println();
            reader.println("IClojure 1.0 -- an enhanced Interactive Clojure");
            reader.println();

            int input = 0;
            String line;

            try {
                while (true) {
                    input += 1;
                    line = reader.readLine(String.format("In [%d]: ", input));
                    if (line.equalsIgnoreCase("exit")) {
                        reset(reader);
                        break;
                    }
                    reader.println(String.format("Out[%d]: %s", input, line));
                    reader.println();
                }
            } finally {
                reset(reader);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to create console reader");
        }
    }
}
