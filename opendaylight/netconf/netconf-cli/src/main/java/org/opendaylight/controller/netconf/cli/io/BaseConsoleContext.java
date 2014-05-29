package org.opendaylight.controller.netconf.cli.io;

import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;

import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class BaseConsoleContext implements ConsoleContext {
    private DataSchemaNode dataSchemaNode;

    public BaseConsoleContext() {
    }

    public BaseConsoleContext(final DataSchemaNode dataSchemaNode) {
        this.dataSchemaNode = dataSchemaNode;
    }

    @Override
    public Completer getCompleter() {
        return new NullCompleter();
    }

    @Override
    public String getPrompt() {
        return dataSchemaNode.getQName().getLocalName();
    }

}
