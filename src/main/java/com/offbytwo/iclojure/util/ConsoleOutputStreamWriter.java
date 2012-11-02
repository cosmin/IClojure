package com.offbytwo.iclojure.util;


import jline.console.ConsoleReader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static java.lang.String.valueOf;


class ConsoleOutputStream extends OutputStream {

    private ConsoleReader reader;

    public ConsoleOutputStream(ConsoleReader reader) {
        this.reader = reader;
    }

    @Override
    public void write(int i) throws IOException {
        reader.print(valueOf((char) i));
    }
}

public class ConsoleOutputStreamWriter extends OutputStreamWriter {
    ConsoleReader reader;

    public ConsoleOutputStreamWriter(ConsoleReader reader) {
        super(new ConsoleOutputStream(reader));
    }
}
