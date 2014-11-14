/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.isSkipInput;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.listType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicDataHolderReader<T extends DataSchemaNode> extends AbstractReader<T> {

    private static final Logger LOG = LoggerFactory.getLogger(BasicDataHolderReader.class);
    private DataHolderCompleter currentCompleter;

    public BasicDataHolderReader(final ConsoleIO console, final SchemaContext schemaContext,
            final boolean readConfigNode) {
        super(console, schemaContext, readConfigNode);
    }

    public BasicDataHolderReader(final ConsoleIO console, final SchemaContext schemaContext) {
        super(console, schemaContext);
    }

    @Override
    public List<Node<?>> readWithContext(final T schemaNode) throws IOException, ReadingException {
        TypeDefinition<?> type = getType(schemaNode);
        console.formatLn("Submit %s %s(%s)", listType(schemaNode), schemaNode.getQName().getLocalName(), type.getQName().getLocalName());

        while (baseTypeFor(type) instanceof UnionTypeDefinition) {
            final Optional<TypeDefinition<?>> optionalTypeDef = new UnionTypeReader(console).read(type);
            if (!optionalTypeDef.isPresent()) {
                return postSkipOperations(schemaNode);
            }
            type = optionalTypeDef.get();
        }

        if (currentCompleter == null) {
            currentCompleter = getBaseCompleter(schemaNode);
        }

        // TODO what if type is leafref, instance-identifier?

        // Handle empty type leaf by question
        if (isEmptyType(type)) {
            final Optional<Boolean> shouldAddEmpty = new DecisionReader().read(console, "Add empty type leaf %s ?",
                    schemaNode.getQName().getLocalName());
            if (shouldAddEmpty.isPresent()) {
                if (shouldAddEmpty.get()) {
                    return wrapValue(schemaNode, "");
                } else {
                    return Collections.emptyList();
                }
            } else {
                return postSkipOperations(schemaNode);
            }
        }

        final String rawValue = readValue();
        if (isSkipInput(rawValue)) {
            return postSkipOperations(schemaNode);
        }

        final Object resolvedValue = currentCompleter.resolveValue(rawValue);

        // Reset state TODO should be in finally
        currentCompleter = null;
        return wrapValue(schemaNode, resolvedValue);
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

    protected abstract TypeDefinition<?> getType(final T schemaNode);

    protected final DataHolderCompleter getBaseCompleter(final T schemaNode) {
        final TypeDefinition<?> type = getType(schemaNode);
        final DataHolderCompleter currentCompleter;

        // Add enum completer
        if (type instanceof EnumTypeDefinition) {
            currentCompleter = new EnumDataHolderCompleter(type);
        } else if (type instanceof IdentityrefTypeDefinition) {
            currentCompleter = new IdentityRefDataHolderCompleter(type, getSchemaContext());
        } else {
            currentCompleter = new GeneralDataHolderCompleter(type);
        }
        this.currentCompleter = currentCompleter;
        return currentCompleter;
    }

    private static interface DataHolderCompleter extends Completer {

        Object resolveValue(String rawValue) throws ReadingException;
    }

    private static class GeneralDataHolderCompleter implements DataHolderCompleter {

        private final Optional<TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>>> codec;
        private final TypeDefinition<?> type;

        public GeneralDataHolderCompleter(final TypeDefinition<?> type) {
            this.type = type;
            codec = getCodecForType(type);
        }

        protected TypeDefinition<?> getType() {
            return type;
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

        @Override
        public Object resolveValue(final String rawValue) throws ReadingException {
            try {
                return codec.isPresent() ? codec.get().deserialize(rawValue) : rawValue;
            } catch (final RuntimeException e) {
                final String message = "It wasn't possible deserialize value " + rawValue + ".";
                LOG.error(message, e);
                throw new ReadingException(message, e);
            }
        }

        @Override
        public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
            return 0;
        }
    }

    private static final class EnumDataHolderCompleter extends GeneralDataHolderCompleter {

        public EnumDataHolderCompleter(final TypeDefinition<?> type) {
            super(type);
        }

        @Override
        public Object resolveValue(final String rawValue) throws ReadingException {
            return super.resolveValue(rawValue);
        }

        @Override
        public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
            return new StringsCompleter(Collections2.transform(((EnumTypeDefinition) getType()).getValues(),
                    new Function<EnumPair, String>() {
                        @Override
                        public String apply(final EnumPair input) {
                            return input.getName();
                        }
                    })).complete(buffer, cursor, candidates);
        }
    }

    private static final class IdentityRefDataHolderCompleter extends GeneralDataHolderCompleter {

        private final BiMap<String, QName> identityMap;

        public IdentityRefDataHolderCompleter(final TypeDefinition<?> type, final SchemaContext schemaContext) {
            super(type);
            this.identityMap = getIdentityMap(schemaContext);
        }

        private static BiMap<String, QName> getIdentityMap(final SchemaContext schemaContext) {
            final BiMap<String, QName> identityMap = HashBiMap.create();
            for (final Module module : schemaContext.getModules()) {
                for (final IdentitySchemaNode identity : module.getIdentities()) {
                    identityMap.put(getIdentityName(identity, module), identity.getQName());
                }
            }
            return identityMap;
        }

        private static String getIdentityName(final IdentitySchemaNode rpcDefinition, final Module module) {
            return IOUtil.qNameToKeyString(rpcDefinition.getQName(), module.getName());
        }

        @Override
        public Object resolveValue(final String rawValue) throws ReadingException {
            final QName qName = identityMap.get(rawValue);
            if (qName == null) {
                throw new ReadingException("No identity found for " + rawValue + " available " + identityMap.keySet());
            }
            return qName;
        }

        @Override
        public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {

            return new StringsCompleter(Collections2.transform(((IdentityrefTypeDefinition) getType()).getIdentity()
                    .getDerivedIdentities(), new Function<IdentitySchemaNode, String>() {
                        @Override
                        public String apply(final IdentitySchemaNode input) {
                            return identityMap.inverse().get(input.getQName());
                        }
                    })).complete(buffer, cursor, candidates);
        }
    }
}
