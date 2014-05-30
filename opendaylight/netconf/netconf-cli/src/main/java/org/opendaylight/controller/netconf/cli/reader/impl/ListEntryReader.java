package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.INF_NO_KEY_IN_LIST;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.EntryConsoleContext;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

class ListEntryReader extends AbstractReader<ListSchemaNode> implements GenericListEntryReader<ListSchemaNode> {
    private static final Logger LOG = LoggerFactory.getLogger(ListEntryReader.class);

    public ListEntryReader(final ConsoleIO console) {
        super(console);
    }

    Set<DataSchemaNode> schemaNodesNotInKey = new HashSet<>();

    @Override
    public List<Node<?>> readInner(final ListSchemaNode listNode) throws IOException, ReadingException {
        final String listName = listNode.getQName().getLocalName();
        final CompositeNodeBuilder<ImmutableCompositeNode> compositeNodeBuilder = ImmutableCompositeNode.builder();
        compositeNodeBuilder.setQName(listNode.getQName());

        console.writeLn("Key values for " + listName);

        final ListKeyReader listReader = new ListKeyReader(console);
        final List<Node<?>> keyNodes = listReader.read(listNode);
        if (keyNodes.size() == listNode.getKeyDefinition().size()) {
            compositeNodeBuilder.addAll(keyNodes);
            if (!schemaNodesNotInKey.isEmpty()) {
                final boolean fillNodesWhichAreNotKey = new DecisionReader().read(console, "Add other nodes to list "
                        + listName + "? [Y|N]");
                if (fillNodesWhichAreNotKey) {
                    final List<Node<?>> newNodes = new GenericReader(console).read(schemaNodesNotInKey);
                    compositeNodeBuilder.addAll(newNodes);
                }
            }
        }

        return Collections.<Node<?>> singletonList(compositeNodeBuilder.toInstance());
    }

    @Override
    protected ConsoleContext getContext(final ListSchemaNode schemaNode) {
        return new EntryConsoleContext();
    }

    private class ListKeyReader extends AbstractReader<ListSchemaNode> {

        public ListKeyReader(final ConsoleIO console) {
            super(console);
        }

        @Override
        public List<Node<?>> readInner(final ListSchemaNode listNode) throws ReadingException {
            if (listNode.getKeyDefinition().isEmpty()) {
                try {
                    console.writeLn(INF_NO_KEY_IN_LIST);
                } catch (final IOException e) {
                    throw new ReadingException("Unable to write data to output for " + listNode.getQName(), e);
                }
            }

            final List<Node<?>> newNodes = new ArrayList<>();

            for (final DataSchemaNode childNode : listNode.getChildNodes()) {
                if (isPartOfKey(childNode, listNode)) {
                    Preconditions.checkArgument(childNode instanceof LeafSchemaNode);
                    try {
                        console.write("Key " + childNode.getQName().getLocalName() + ". ");
                    } catch (final IOException e) {
                        throw new ReadingException("Unable to write data to output for " + listNode.getQName(), e);
                    }
                    final List<Node<?>> newNode = new LeafReader(console).read((LeafSchemaNode) childNode);
                    if (newNode.get(0).getValue().equals(SKIP)) {
                        return Collections.emptyList();
                    }
                    newNodes.addAll(newNode);
                } else {
                    schemaNodesNotInKey.add(childNode);
                }
            }
            return newNodes;
        }

        @Override
        protected ConsoleContext getContext(final ListSchemaNode schemaNode) {
            return new BaseConsoleContext();
        }

        private boolean isPartOfKey(final SchemaNode candidateToKey, final ListSchemaNode listNode) {
            for (final QName keyQName : listNode.getKeyDefinition()) {
                if (candidateToKey.getQName().equals(keyQName)) {
                    return true;
                }
            }
            return false;
        }
    }

}
