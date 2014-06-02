package org.opendaylight.controller.netconf.cli.io;

import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;

public class EntryConsoleContext implements ConsoleContext {
    private int entryNumber;

    public EntryConsoleContext() {
        super();
    }

    @Override
    public Completer getCompleter() {
        return new NullCompleter();
    }

    @Override
    public String getPrompt() {
        return "[" + entryNumber + "]";
    }

    public int getEntryNumber() {
        return entryNumber;
    }

    public void setEntryNumber(final int entryNumber) {
        this.entryNumber = entryNumber;
    }
}
