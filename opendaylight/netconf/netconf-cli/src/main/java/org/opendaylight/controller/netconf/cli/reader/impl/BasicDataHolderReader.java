package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.listType;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;

public abstract class BasicDataHolderReader<T extends DataSchemaNode> extends AbstractReader<T> {

    public BasicDataHolderReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> readInner(final T schemaNode) throws IOException, ReadingException {
        TypeDefinition<?> type = getType(schemaNode);
        console.formatLn("%s %s(%s)", listType(schemaNode), schemaNode.getQName().getLocalName(), type.getQName().getLocalName());

        if (baseTypeFor(type) instanceof UnionTypeDefinition) {
            type = new UnionTypeReader(console).read(type);
        }

        // TODO input validation - IncorrectValueException
        // TODO what if type is leafref, identityref, instance-identifier?

        // Handle empty type leaf by question
        if(type instanceof EmptyTypeDefinition) {
            final boolean shouldAddEmpty = new DecisionReader().read(console, "Add empty type leaf %s ?", schemaNode.getQName().getLocalName());
            if(shouldAddEmpty) {
                return wrapValue(schemaNode, "");
            } else {
                return Collections.emptyList();
            }
        }

        final Optional<TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>>> codec = getCodecForType(type);
        final String rawValue = readValue();

        if(isSkipInput(rawValue)) {
            console.formatLn("Skipping %s", schemaNode.getQName());
            return Collections.emptyList();
        }

        return wrapValue(schemaNode, codec.isPresent() ? codec.get().deserialize(rawValue) : rawValue);
    }

    private TypeDefinition<?> baseTypeFor(final TypeDefinition<?> type) {
        if (type.getBaseType() != null) {
            return baseTypeFor(type.getBaseType());
        }
        return type;
    }

    protected String readValue() throws IOException {
        return console.read();
    }

    private List<Node<?>> wrapValue(final T schemaNode, final Object defaultValue) {
        final Node<?> newNode = NodeFactory.createImmutableSimpleNode(schemaNode.getQName(), null, defaultValue);
        return Collections.<Node<?>> singletonList(newNode);
    }

    private Optional<TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>>> getCodecForType(final TypeDefinition<?> type) {
        if (type != null) {
            return Optional.<TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>>>fromNullable(TypeDefinitionAwareCodec.from(type));
        }
        return Optional.absent();
    }

    protected abstract TypeDefinition<?> getType(final T schemaNode);
}
