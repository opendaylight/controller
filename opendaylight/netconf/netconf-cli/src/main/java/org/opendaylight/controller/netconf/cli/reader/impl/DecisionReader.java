package org.opendaylight.controller.netconf.cli.reader.impl;

import java.io.IOException;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;

public class DecisionReader {
    private static final String YES = "Y";

    public boolean read(final ConsoleIO console, final String questionMessage,final Object ... objects) throws IOException {
        final ConsoleContext ctx = getContext();
        console.enterContext(ctx);
        try {
            console.writeLn("");
            console.writeLn(String.format(questionMessage,objects));
            final String rawConsoleValue = console.read();
            return YES.equals(rawConsoleValue.toUpperCase());
        } finally {
            console.leaveContext();
        }
    }

    private static ConsoleContext getContext() {
        return new ConsoleContext() {

            @Override
            public String getPrompt() {
                return null;
            }

            @Override
            public Completer getCompleter() {
                return new StringsCompleter("Y", "N");
            }
        };
    }

}
