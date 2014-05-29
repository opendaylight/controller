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
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

import com.google.common.base.Preconditions;

public class ListKeyReader extends AbstractReader<ListSchemaNode> {

    Set<DataSchemaNode> nodesNotPartOfKey = Collections.emptySet();

    public ListKeyReader(ConsoleIO console) {
        super(console);
    }

    // FIXME is dedicated reader for list keys necessary ?

    // FIXME why override read instead of readInner ?

    @Override
    public List<Node<?>> readInner(ListSchemaNode listNode) throws ReadingException {
        if (listNode.getKeyDefinition().isEmpty()) {
            try {
                console.writeLn(INF_NO_KEY_IN_LIST);
            } catch (IOException e) {
                throw new ReadingException("Unable to write data to output for " + listNode.getQName(), e);
            }
        }

        Set<DataSchemaNode> nodesWhichAreNotKey = new HashSet<>();
        List<Node<?>> newNodes = new ArrayList<>();

        for (DataSchemaNode childNode : listNode.getChildNodes()) {
            if (isPartOfKey(childNode, listNode)) {
                Preconditions.checkArgument(childNode instanceof LeafSchemaNode);
                try {
                    console.write("Key " + childNode.getQName().getLocalName() + ". ");
                } catch (IOException e) {
                    throw new ReadingException("Unable to write data to output for " + listNode.getQName(), e);
                }
                List<Node<?>> newNode = new LeafReader(console).read((LeafSchemaNode) childNode);
                if (newNode.get(0).getValue().equals(SKIP)) {
                    return Collections.emptyList();
                }
                newNodes.addAll(newNode);
            } else {
                nodesWhichAreNotKey.add(childNode);
            }
        }
        nodesNotPartOfKey = nodesWhichAreNotKey;
        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final ListSchemaNode schemaNode) {
        return new BaseConsoleContext();
    }

    public Set<DataSchemaNode> getNotKeySchemaNodes() {
        return nodesNotPartOfKey;
    }

    private boolean isPartOfKey(SchemaNode candidateToKey, ListSchemaNode listNode) {
        for (QName keyQName : listNode.getKeyDefinition()) {
            if (candidateToKey.getQName().equals(keyQName)) {
                return true;
            }
        }
        return false;
    }
}
