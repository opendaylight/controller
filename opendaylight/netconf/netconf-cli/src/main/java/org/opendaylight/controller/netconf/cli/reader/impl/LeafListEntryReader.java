package org.opendaylight.controller.netconf.cli.reader.impl;

import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.EntryConsoleContext;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

class LeafListEntryReader extends BasicDataHolderReader<LeafListSchemaNode> implements
        GenericListEntryReader<LeafListSchemaNode> {

    public LeafListEntryReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> read(final LeafListSchemaNode schemaNode) throws ReadingException {
        return super.read(schemaNode);
    }

    @Override
    protected ConsoleContext getContext(final LeafListSchemaNode schemaNode) {
        return new EntryConsoleContext();
    }

}
