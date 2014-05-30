package org.opendaylight.controller.netconf.cli.io;

import java.io.IOException;

public interface ConsoleIO {
    String read() throws IOException;

    void write(CharSequence data) throws IOException;

    void writeLn(CharSequence data) throws IOException;

    void enterContext(ConsoleContext consoleContext);

    void leaveContext();

    ConsoleContext getContext();

}
