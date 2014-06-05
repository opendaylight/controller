package org.opendaylight.controller.netconf.cli.reader.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import jline.console.completer.Completer;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

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
        return new BaseConsoleContext<LeafListSchemaNode>(schemaNode) {

            @Override
            public Optional<String> getPrompt() {
                return Optional.of("[entry]");
            }

            @Override
            protected List<Completer> getAdditionalCompleters() {
                return Lists.newArrayList(getBaseCompleter(getDataSchemaNode()));
            }
        };
    }
}
