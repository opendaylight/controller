/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.netconf.cli.CommandArgHandlerRegistry;
import org.opendaylight.controller.netconf.cli.commands.CommandConstants;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.Reader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

public class GenericReader extends AbstractReader<DataSchemaNode> {

    private final CommandArgHandlerRegistry argumentHandlerRegistry;

    public GenericReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry,
            final SchemaContext schemaContext) {
        super(console, schemaContext);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    public GenericReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry,
            final SchemaContext schemaContext, final boolean readConfigNode) {
        super(console, schemaContext, readConfigNode);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    @Override
    protected List<Node<?>> readWithContext(final DataSchemaNode schemaNode) throws IOException, ReadingException {
        final Optional<Class<? extends Reader<DataSchemaNode>>> customReaderClassOpt = tryGetCustomHandler(schemaNode);

        if (customReaderClassOpt.isPresent()) {
            // TODO resolve class cast of generic custom readers
            final Reader<DataSchemaNode> customReaderInstance = (Reader<DataSchemaNode>) argumentHandlerRegistry
                    .getCustomReader(customReaderClassOpt.get());
            Preconditions.checkNotNull(customReaderInstance, "Unknown custom reader: %s", customReaderClassOpt.get());
            return customReaderInstance.read(schemaNode);
        } else {
            return readGeneric(schemaNode);
        }

        // TODO reuse instances
    }

    private List<Node<?>> readGeneric(final DataSchemaNode schemaNode) throws ReadingException, IOException {
        final List<Node<?>> newNodes = new ArrayList<>();
        boolean isRedCorrectly = false;
        do {
            try {
                if (schemaNode instanceof LeafSchemaNode) {
                    return new LeafReader(console, getSchemaContext(), getReadConfigNode())
                            .read((LeafSchemaNode) schemaNode);
                } else if (schemaNode instanceof ContainerSchemaNode) {
                    return new ContainerReader(console, argumentHandlerRegistry, getSchemaContext(),
                            getReadConfigNode()).read((ContainerSchemaNode) schemaNode);
                } else if (schemaNode instanceof ListSchemaNode) {
                    final GenericListEntryReader<ListSchemaNode> entryReader = new ListEntryReader(console,
                            argumentHandlerRegistry, getSchemaContext(), getReadConfigNode());
                    return new GenericListReader<>(console, entryReader, getSchemaContext(), getReadConfigNode())
                            .read((ListSchemaNode) schemaNode);
                } else if (schemaNode instanceof LeafListSchemaNode) {
                    final GenericListEntryReader<LeafListSchemaNode> entryReader = new LeafListEntryReader(console,
                            getSchemaContext(), getReadConfigNode());
                    return new GenericListReader<>(console, entryReader, getSchemaContext(), getReadConfigNode())
                            .read((LeafListSchemaNode) schemaNode);
                } else if (schemaNode instanceof ChoiceSchemaNode) {
                    return new ChoiceReader(console, argumentHandlerRegistry, getSchemaContext(), getReadConfigNode())
                            .read((ChoiceSchemaNode) schemaNode);
                } else if (schemaNode instanceof AnyXmlSchemaNode) {
                    return new AnyXmlReader(console, getSchemaContext(), getReadConfigNode())
                            .read((AnyXmlSchemaNode) schemaNode);
                }
                isRedCorrectly = true;
            } catch (final ReadingException e) {
                console.writeLn(e.getMessage());
            }
        } while (!isRedCorrectly);
        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final DataSchemaNode schemaNode) {
        // return null context, leave context to specific implementations
        return NULL_CONTEXT;
    }

    private <T> Optional<Class<? extends T>> tryGetCustomHandler(final DataSchemaNode dataSchemaNode) {

        for (final UnknownSchemaNode unknownSchemaNode : dataSchemaNode.getUnknownSchemaNodes()) {

            if (isExtenstionForCustomHandler(unknownSchemaNode)) {
                final String argumentHandlerClassName = unknownSchemaNode.getNodeParameter();
                try {
                    final Class<?> argumentClass = Class.forName(argumentHandlerClassName);
                    // TODO add check before cast
                    return Optional.<Class<? extends T>> of((Class<? extends T>) argumentClass);
                } catch (final ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unknown custom reader class " + argumentHandlerClassName
                            + " for: " + dataSchemaNode.getQName());
                }
            }
        }

        return Optional.absent();
    }

    private boolean isExtenstionForCustomHandler(final UnknownSchemaNode unknownSchemaNode) {
        final QName qName = unknownSchemaNode.getExtensionDefinition().getQName();
        return qName.equals(CommandConstants.ARG_HANDLER_EXT_QNAME);
    }
}
