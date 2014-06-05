package org.opendaylight.controller.netconf.cli.reader.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.netconf.cli.CommandArgHandlerRegistry;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ListEntryReader extends AbstractReader<ListSchemaNode> implements GenericListEntryReader<ListSchemaNode> {
    private static final Logger LOG = LoggerFactory.getLogger(ListEntryReader.class);

    private final CommandArgHandlerRegistry argumentHandlerRegistry;

    public ListEntryReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry) {
        super(console);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    @Override
    public List<Node<?>> readInner(final ListSchemaNode listNode) throws IOException, ReadingException {
        final String listName = listNode.getQName().getLocalName();
        final CompositeNodeBuilder<ImmutableCompositeNode> compositeNodeBuilder = ImmutableCompositeNode.builder();
        compositeNodeBuilder.setQName(listNode.getQName());

        final SeparatedNodes separatedChildNodes = separateNodes(listNode);

        final List<Node<?>> nodes = readKeys(separatedChildNodes.getKeyNodes());

        if(separatedChildNodes.getNonKeyNodes().isEmpty() == false) {
            // TODO ask if other values should be read only if keys.empty == false
            final boolean readNodesWhichAreNotKey = new DecisionReader().read(console, "Add non-key nodes to list %s? [Y|N]", listName);
            if (readNodesWhichAreNotKey) {
                nodes.addAll(readNotKeys(separatedChildNodes.getNonKeyNodes()));
            }
        }

        if (!nodes.isEmpty()) {
            compositeNodeBuilder.addAll(nodes);
            return Collections.<Node<?>> singletonList(compositeNodeBuilder.toInstance());
        } else {
            return Collections.emptyList();
        }
    }

    private static final class SeparatedNodes {
        private final Set<DataSchemaNode> keyNodes;
        private final Set<DataSchemaNode> nonKeyNodes;

        public SeparatedNodes(final Set<DataSchemaNode> keyNodes, final Set<DataSchemaNode> nonKeyNodes) {
            this.keyNodes = keyNodes;
            this.nonKeyNodes = nonKeyNodes;
        }

        public Set<DataSchemaNode> getKeyNodes() {
            return keyNodes;
        }

        public Set<DataSchemaNode> getNonKeyNodes() {
            return nonKeyNodes;
        }
    }

    private SeparatedNodes separateNodes(final ListSchemaNode listSchemaNode) {
        final Set<DataSchemaNode> keys = new HashSet<>();
        final Set<DataSchemaNode> nonKeys = new HashSet<>();

        final Set<QName> keyQNames = Sets.newHashSet(listSchemaNode.getKeyDefinition());

        for (final DataSchemaNode dataSchemaNode : listSchemaNode.getChildNodes()) {
            if(keyQNames.contains(dataSchemaNode.getQName())) {
                Preconditions.checkArgument(dataSchemaNode instanceof LeafSchemaNode);
                keys.add(dataSchemaNode);
            } else {
                nonKeys.add(dataSchemaNode);
            }
        }

        return new SeparatedNodes(keys, nonKeys);
    }

    private List<Node<?>> readKeys(final Set<DataSchemaNode> keys) throws ReadingException {
        final List<Node<?>> newNodes = new ArrayList<>();

        for (final DataSchemaNode key : keys) {
            final List<Node<?>> readKey = new LeafReader(console).read((LeafSchemaNode) key);
            Preconditions.checkArgument(readKey.size() == 1, "Value for key has to be set");
            newNodes.addAll(readKey);
        }
        return newNodes;
    }

    private List<Node<?>> readNotKeys(final Set<DataSchemaNode> notKeys) throws ReadingException {
        final List<Node<?>> newNodes = new ArrayList<>();
        for (final DataSchemaNode notKey : notKeys) {
            newNodes.addAll(argumentHandlerRegistry.getGenericReader().read(notKey));
        }
        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final ListSchemaNode schemaNode) {
        return new BaseConsoleContext<ListSchemaNode>(schemaNode) {
            @Override
            public Optional<String> getPrompt() {
                return Optional.of("[entry]");
            }
        };
    }

}
