package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

public class LeafReader extends BasicDataHolderReader<LeafSchemaNode> {

    public LeafReader(ConsoleIO console) {
        super(console);
    }

    @Override
    protected ConsoleContext getContext(final LeafSchemaNode schemaNode) {
        return new BaseConsoleContext(schemaNode) {

            @Override
            public Completer getCompleter() {
                return new StringsCompleter(SKIP);
            }
        };
    }

}
