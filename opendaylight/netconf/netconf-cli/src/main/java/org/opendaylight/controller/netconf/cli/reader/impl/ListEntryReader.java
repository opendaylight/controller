package org.opendaylight.controller.netconf.cli.reader.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ListEntryReader extends AbstractReader<ListSchemaNode> implements GenericListEntryReader<ListSchemaNode> {
    private static final Logger LOG = LoggerFactory.getLogger(ListEntryReader.class);
    int entryCount = 1;

    public ListEntryReader(ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> readInner(ListSchemaNode listNode) throws IOException, ReadingException {
        String listName = listNode.getQName().getLocalName();
        CompositeNodeBuilder<ImmutableCompositeNode> compositeNodeBuilder = ImmutableCompositeNode.builder();
        compositeNodeBuilder.setQName(listNode.getQName());

        console.writeLn("Key values for " + listName + " [entry " + entryCount + "]");

        ListKeyReader listReader = new ListKeyReader(console);
        List<Node<?>> keyNodes = listReader.read(listNode);
        if (keyNodes.size() == listNode.getKeyDefinition().size()) {
            compositeNodeBuilder.addAll(keyNodes);
            Set<DataSchemaNode> schemaNodesNotInKey = listReader.getNotKeySchemaNodes();
            if (!schemaNodesNotInKey.isEmpty()) {
                boolean fillNodesWhichAreNotKey = DecisionReader.read(console, "Add other nodes to list " + listName
                        + " [entry " + entryCount + "]" + "? [Y|N]");
                if (fillNodesWhichAreNotKey) {
                    List<Node<?>> newNodes = new GenericReader(console).read(schemaNodesNotInKey);
                    compositeNodeBuilder.addAll(newNodes);
                }
            }
        }

        CompositeNode newNode = compositeNodeBuilder.toInstance();
        List<Node<?>> newNodes = new ArrayList<>();
        newNodes.add(newNode);
        entryCount++;
        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final ListSchemaNode schemaNode) {
        return new BaseConsoleContext() {
            @Override
            public String getPrompt() {
                return schemaNode.getQName().getLocalName() + "[" + entryCount + "]";
            }
        };
    }

}
