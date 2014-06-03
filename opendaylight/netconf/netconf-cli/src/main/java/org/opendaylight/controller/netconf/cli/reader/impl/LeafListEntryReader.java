package org.opendaylight.controller.netconf.cli.reader.impl;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

import com.google.common.base.Optional;

class LeafListEntryReader extends BasicDataHolderReader<LeafListSchemaNode> implements
        GenericListEntryReader<LeafListSchemaNode> {

    public LeafListEntryReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    protected TypeDefinition<?> getType(final LeafListSchemaNode schemaNode) {
        return schemaNode.getType();
    }

    @Override
    protected ConsoleContext getContext(final LeafListSchemaNode schemaNode) {
        return new EntryBaseConsoleContext(schemaNode);
    }

    static final class EntryBaseConsoleContext extends BaseConsoleContext<DataSchemaNode> {
        public EntryBaseConsoleContext(final DataSchemaNode schemaNode) {
            super(schemaNode);
        }

        @Override
        public Optional<String> getPrompt() {
            return Optional.of("[entry]");
        }
    }
}
