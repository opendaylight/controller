package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.listType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public abstract class BasicDataHolderReader<T extends DataSchemaNode> extends AbstractReader<T> {

    public BasicDataHolderReader(ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> readInner(T schemaNode) throws IOException, ReadingException {
        TypeDefinition<?> type = getType(schemaNode);
        console.writeLn(listType(schemaNode) + " " + schemaNode.getQName().getLocalName() + " ("
                + type.getQName().getLocalName() + ") ");

        // TODO input validation - IncorrectValueException

        String rawValue = console.read();
        // TODO what if type is leafref, identityref, instance-identifier?

        TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codec = getCodecForType(type);
        Object valueForWrite = codec != null ? codec.deserialize(rawValue) : rawValue;

        Node<?> newNode = null;
        if (!rawValue.equals(SKIP)) {
            newNode = NodeFactory.createImmutableSimpleNode(schemaNode.getQName(), null, valueForWrite);
        }
        List<Node<?>> newNodes = new ArrayList<>();
        newNodes.add(newNode);
        return newNodes;
    }

    private TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> getCodecForType(TypeDefinition<?> type) {
        if (type != null) {
            return TypeDefinitionAwareCodec.from(type);
        }
        return null;

    }

    private TypeDefinition<?> getType(DataSchemaNode schemaNode) {
        if (schemaNode instanceof LeafSchemaNode) {
            return ((LeafSchemaNode) schemaNode).getType();
        } else if (schemaNode instanceof LeafListSchemaNode) {
            return ((LeafListSchemaNode) schemaNode).getType();
        }
        return null;
    }
}
