package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.listType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.ListConsoleContext;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericListReader<T extends DataSchemaNode> extends AbstractReader<T> {
    private static final Logger LOG = LoggerFactory.getLogger(GenericListReader.class);

    private final GenericListEntryReader<T> concreteListEntryReader;

    public GenericListReader(final ConsoleIO console, final GenericListEntryReader<T> concreteListEntryReader) {
        super(console);
        this.concreteListEntryReader = concreteListEntryReader;
    }

    @Override
    public List<Node<?>> readInner(final T schemaNode) throws IOException, ReadingException {
        final List<Node<?>> newNodes = new ArrayList<>();
        boolean readNextListEntry = true;
        while (readNextListEntry) {
            newNodes.addAll(concreteListEntryReader.read(schemaNode));
            readNextListEntry = new DecisionReader().read(console, "Add other entry to " + listType(schemaNode) + " "
                    + schemaNode.getQName().getLocalName() + " " + " [Y|N]?");
        }
        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final T schemaNode) {
        return new ListConsoleContext(schemaNode);
    }

}
