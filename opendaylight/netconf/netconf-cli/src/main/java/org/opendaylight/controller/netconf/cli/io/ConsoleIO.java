package org.opendaylight.controller.netconf.cli.io;

import java.io.IOException;

/**
 * Definition of IO interface
 */
public interface ConsoleIO {

    String read() throws IOException;

    String read(Character mask) throws IOException;

    void write(CharSequence data) throws IOException;

    void writeLn(CharSequence data) throws IOException;

    void formatLn(String format, Object... args) throws IOException;

    void enterContext(ConsoleContext consoleContext);

    void leaveContext();

}
