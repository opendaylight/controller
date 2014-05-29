package org.opendaylight.controller.netconf.cli.reader.impl;

import java.util.List;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

class LeafListEntryReader extends BasicDataHolderReader<LeafListSchemaNode> implements
        GenericListEntryReader<LeafListSchemaNode> {

    // FIXME this cannot be here
    int entryCount = 0;

    public LeafListEntryReader(ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> read(LeafListSchemaNode schemaNode) throws ReadingException {
        entryCount++;
        return super.read(schemaNode);

    }

    @Override
    protected ConsoleContext getContext(final LeafListSchemaNode schemaNode) {
        return new BaseConsoleContext() {

            @Override
            public String getPrompt() {
                return schemaNode.getQName().getLocalName() + "[" + entryCount + "]";
            }

        };
    }

}
