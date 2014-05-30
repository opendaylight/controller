package org.opendaylight.controller.netconf.cli.reader.impl;

import org.opendaylight.controller.netconf.cli.io.BaseDataHolderConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

public class LeafReader extends BasicDataHolderReader<LeafSchemaNode> {

    public LeafReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    protected ConsoleContext getContext(final LeafSchemaNode schemaNode) {
        return new BaseDataHolderConsoleContext(schemaNode);
    }
}
