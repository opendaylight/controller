package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.isSkipInput;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.listType;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;

public abstract class BasicDataHolderReader<T extends DataSchemaNode> extends AbstractReader<T> {

    private static final Logger LOG = LoggerFactory.getLogger(BasicDataHolderReader.class);

    public BasicDataHolderReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> readWithContext(final T schemaNode) throws IOException, ReadingException {
        TypeDefinition<?> type = getType(schemaNode);
        console.formatLn("%s %s(%s)", listType(schemaNode), schemaNode.getQName().getLocalName(), type.getQName()
                .getLocalName());

        while (baseTypeFor(type) instanceof UnionTypeDefinition) {
            final Optional<TypeDefinition<?>> optionalTypeDef = new UnionTypeReader(console).read(type);
            if (!optionalTypeDef.isPresent()) {
                return postSkipOperations(schemaNode);
            }
            type = optionalTypeDef.get();
        }

        // TODO input validation - IncorrectValueException
        // TODO what if type is leafref, identityref, instance-identifier?

        // Handle empty type leaf by question
        if (type instanceof EmptyTypeDefinition) {
            final boolean shouldAddEmpty = new DecisionReader().read(console, "Add empty type leaf %s ?", schemaNode
                    .getQName().getLocalName());
            if (shouldAddEmpty) {
                return wrapValue(schemaNode, "");
            } else {
                return Collections.emptyList();
            }
        }

        final Optional<TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>>> codec = getCodecForType(type);
        final String rawValue = readValue();

        if (isSkipInput(rawValue)) {
            return postSkipOperations(schemaNode);
        }

        final Object finalValue;
        if (type instanceof IdentityrefTypeDefinition) {
            finalValue = QName.create(rawValue);
        } else if (codec.isPresent()) {
            try {
                finalValue = codec.get().deserialize(rawValue);
            } catch (final RuntimeException e) {
                // FIXME this is not how to log exceptions, exceptions are logged this way LOG.error("message", e)
                LOG.error(getStackTrace(e));
                throw new ReadingException("It wasn't possible to deserialize value " + rawValue + ".", e);
            }

        } else {
            finalValue = rawValue;
        }

        return wrapValue(schemaNode, finalValue);
    }

    private List<Node<?>> postSkipOperations(final DataSchemaNode schemaNode) throws IOException {
        console.formatLn("Skipping %s", schemaNode.getQName());
        return Collections.emptyList();
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

    private List<Node<?>> wrapValue(final T schemaNode, final Object value) {
        final Node<?> newNode = NodeFactory.createImmutableSimpleNode(schemaNode.getQName(), null, value);
        return Collections.<Node<?>> singletonList(newNode);
    }

    private Optional<TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>>> getCodecForType(
            final TypeDefinition<?> type) {
        if (type != null) {
            return Optional
                    .<TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>>> fromNullable(TypeDefinitionAwareCodec
                            .from(type));
        }
        return Optional.absent();
    }

    protected abstract TypeDefinition<?> getType(final T schemaNode);

    protected final Completer getBaseCompleter(final T schemaNode) {
        final TypeDefinition<?> type = getType(schemaNode);

        // Add enum completer
        if (type instanceof EnumTypeDefinition) {
            return new StringsCompleter(Collections2.transform(((EnumTypeDefinition) type).getValues(),
                    new Function<EnumPair, String>() {
                        @Override
                        public String apply(final EnumPair input) {
                            return input.getName();
                        }
                    }));
        }

        if (type instanceof IdentityrefTypeDefinition) {
            return new StringsCompleter(Collections2.transform(((IdentityrefTypeDefinition) type).getIdentity()
                    .getDerivedIdentities(), new Function<IdentitySchemaNode, String>() {
                @Override
                public String apply(final IdentitySchemaNode input) {
                    return input.getQName().toString();
                }
            }));

        }

        return new StringsCompleter();
    }
}
