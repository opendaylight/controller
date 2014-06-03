package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.MSG_NO_KEY_IN_LIST;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.MSG_NO_NOT_KEY_IN_LIST;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.QUEST_ADD_OTHERS_NODE;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private enum ListNode {
        key, notKey
    }

    public ListEntryReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> readInner(final ListSchemaNode listNode) throws IOException, ReadingException {
        final String listName = listNode.getQName().getLocalName();
        final CompositeNodeBuilder<ImmutableCompositeNode> compositeNodeBuilder = ImmutableCompositeNode.builder();
        compositeNodeBuilder.setQName(listNode.getQName());

        final List<Set<DataSchemaNode>> separatedChildNodes = separateNodes(listNode);

        final List<Node<?>> nodes = readKeys(separatedChildNodes.get(ListNode.key.ordinal()));
        if (isKeyPartRedCorrectly(nodes, listNode)
                && existsSomethingElseForRead(separatedChildNodes.get(ListNode.notKey.ordinal()))) {
            final boolean readNodesWhichAreNotKey = new DecisionReader().read(console, QUEST_ADD_OTHERS_NODE, listName);
            if (readNodesWhichAreNotKey) {
                nodes.addAll(readNotKeys(separatedChildNodes.get(ListNode.notKey.ordinal())));
            }
        }
        if (!nodes.isEmpty()) {
            compositeNodeBuilder.addAll(nodes);
            return Collections.<Node<?>> singletonList(compositeNodeBuilder.toInstance());
        } else {
            return Collections.emptyList();
        }
    }

    private boolean existsSomethingElseForRead(final Set<DataSchemaNode> notKeys) {
        return !notKeys.isEmpty();
    }

    private List<Set<DataSchemaNode>> separateNodes(final ListSchemaNode listSchemaNode) {
        final List<Set<DataSchemaNode>> separatedListNodes = new ArrayList<>();
        final Set<DataSchemaNode> keys = new HashSet<>();
        final Set<DataSchemaNode> notKeys = new HashSet<>();

        for (final DataSchemaNode childNode : listSchemaNode.getChildNodes()) {
            if (isPartOfKey(childNode, listSchemaNode)) {
                keys.add(childNode);
            } else {
                notKeys.add(childNode);
            }
        }
        separatedListNodes.add(ListNode.key.ordinal(), keys);
        separatedListNodes.add(ListNode.notKey.ordinal(), notKeys);
        return separatedListNodes;
    }

    private List<Node<?>> readKeys(final Set<DataSchemaNode> keys) throws ReadingException {
        if (keys.isEmpty()) {
            LOG.info(MSG_NO_KEY_IN_LIST);
        }

        final List<Node<?>> newNodes = new ArrayList<>();

        for (final DataSchemaNode key : keys) {
            Preconditions.checkArgument(key instanceof LeafSchemaNode);
            try {
                console.write("Key " + key.getQName().getLocalName() + ". ");
            } catch (final IOException e) {
                throw new ReadingException("Unable to write data to output for " + key.getQName(), e);
            }
            final List<Node<?>> newNode = new LeafReader(console).read((LeafSchemaNode) key);
            if (newNode.get(0).getValue().equals(SKIP)) {
                return Collections.emptyList();
            }
            newNodes.addAll(newNode);
        }
        return newNodes;
    }

    private List<Node<?>> readNotKeys(final Set<DataSchemaNode> notKeys) throws ReadingException {
        if (notKeys.isEmpty()) {
            LOG.info(MSG_NO_NOT_KEY_IN_LIST);
        }

        final List<Node<?>> newNodes = new ArrayList<>();
        for (final DataSchemaNode notKey : notKeys) {
            newNodes.addAll(new GenericReader(console).read(notKey));
        }
        return newNodes;
    }

    private boolean isKeyPartRedCorrectly(final List<Node<?>> redKeys, final ListSchemaNode listNode) {
        return !(redKeys.isEmpty() && !listNode.getKeyDefinition().isEmpty());
    }

    private boolean isPartOfKey(final SchemaNode candidateToKey, final ListSchemaNode listNode) {
        for (final QName keyQName : listNode.getKeyDefinition()) {
            if (candidateToKey.getQName().equals(keyQName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ConsoleContext getContext(final ListSchemaNode schemaNode) {
        return new EntryConsoleContext();
    }

}
