package org.opendaylight.controller.netconf.cli.reader.impl;

import java.io.IOException;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericListReader<T extends DataSchemaNode> extends AbstractReader<T> {
    private static final Logger LOG = LoggerFactory.getLogger(GenericListReader.class);

    private GenericListEntryReader<T> concreteListEntryReader;

    public GenericListReader(ConsoleIO console, GenericListEntryReader<T> concreteListEntryReader) {
        super(console);
        this.concreteListEntryReader = concreteListEntryReader;
    }

    public List<Node<?>> readInner(T schemaNode) throws IOException, ReadingException {
        return concreteListEntryReader.read(schemaNode);
    }

    @Override
    protected ConsoleContext getContext(T schemaNode) {
        return new BaseConsoleContext() {

            @Override
            public String getPrompt() {
                return null;
            }
        };
    }

    // read more elements at once
    // public List<Node<?>> readInner(T schemaNode) throws IOException,
    // IncorrectValueException {
    // List<Node<?>> newNodes = new ArrayList<>();
    // boolean readNextListEntry = true;
    // while (readNextListEntry) {
    // newNodes.addAll(concreteListEntryReader.read(schemaNode));
    // readNextListEntry = DecisionReader.read(console, "Add other entry to " +
    // listType(schemaNode) + " "
    // + schemaNode.getQName().getLocalName() + " " + " [Y|N]?");
    // }
    // return newNodes;
    // }

}
