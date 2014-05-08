/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.Reader;
import org.opendaylight.controller.netconf.cli.reader.custom.ConfigReader;
import org.opendaylight.controller.netconf.cli.reader.custom.EditContentReader;
import org.opendaylight.controller.netconf.cli.reader.custom.FilterReader;
import org.opendaylight.controller.netconf.cli.reader.custom.PasswordReader;
import org.opendaylight.controller.netconf.cli.reader.impl.GenericReader;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.controller.netconf.cli.writer.Writer;
import org.opendaylight.controller.netconf.cli.writer.custom.DataWriter;
import org.opendaylight.controller.netconf.cli.writer.impl.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Keeps custom and generic input/output arguments handlers. Custom handlers are
 * constructed lazily, due to remote schema context acquisition.
 */
public class CommandArgHandlerRegistry {

    private final ConsoleIO consoleIO;
    private final SchemaContextRegistry schemaContextRegistry;

    private final Map<Class<? extends Reader<? extends DataSchemaNode>>, ReaderProvider> customReaders = Maps
            .newHashMap();
    private final Map<Class<? extends Writer<DataSchemaNode>>, WriterProvider> customWriters = Maps.newHashMap();

    public CommandArgHandlerRegistry(final ConsoleIO consoleIO, final SchemaContextRegistry schemaContextRegistry) {
        this.consoleIO = consoleIO;
        this.schemaContextRegistry = schemaContextRegistry;

        setUpReaders();
        setUpWriters();
    }

    private void setUpWriters() {
        customWriters.put(DataWriter.class, new DataWriterProvider());
    }

    private void setUpReaders() {
        customReaders.put(PasswordReader.class, new PasswordReaderProvider());
        customReaders.put(FilterReader.class, new FilterReaderProvider());
        customReaders.put(ConfigReader.class, new ConfigReaderProvider());
        customReaders.put(EditContentReader.class, new EditContentReaderProvider());
    }

    public synchronized Reader<? extends DataSchemaNode> getCustomReader(
            final Class<? extends Reader<DataSchemaNode>> readerType) {
        return customReaders.get(readerType).provide(consoleIO, this, schemaContextRegistry);
    }

    private static SchemaContext getRemoteSchema(final Class<?> handlerType,
            final SchemaContextRegistry schemaContextRegistry) {
        final Optional<SchemaContext> remoteSchemaContext = schemaContextRegistry.getRemoteSchemaContext();
        Preconditions.checkState(remoteSchemaContext.isPresent(),
                "Remote schema context not acquired yet, cannot get handler %s", handlerType);
        return remoteSchemaContext.get();
    }

    public synchronized Reader<DataSchemaNode> getGenericReader(final SchemaContext schemaContext) {
        return new GenericReader(consoleIO, this, schemaContext);
    }

    public synchronized Reader<DataSchemaNode> getGenericReader(final SchemaContext schemaContext,
            final boolean readConfigNode) {
        return new GenericReader(consoleIO, this, schemaContext, readConfigNode);
    }

    public synchronized Writer<DataSchemaNode> getCustomWriter(final Class<? extends Writer<DataSchemaNode>> writerType) {
        return customWriters.get(writerType).provide(consoleIO, getRemoteSchema(writerType, schemaContextRegistry),
                this);
    }

    public synchronized Writer<DataSchemaNode> getGenericWriter() {
        return new NormalizedNodeWriter(consoleIO, new OutFormatter());
    }

    /**
     * Reader providers, in order to construct readers lazily
     */
    private static interface ReaderProvider {
        Reader<? extends DataSchemaNode> provide(ConsoleIO consoleIO,
                final CommandArgHandlerRegistry commandArgHandlerRegistry,
                final SchemaContextRegistry schemaContextRegistry);
    }

    private static final class FilterReaderProvider implements ReaderProvider {
        @Override
        public Reader<? extends DataSchemaNode> provide(final ConsoleIO consoleIO,
                final CommandArgHandlerRegistry commandArgHandlerRegistry,
                final SchemaContextRegistry schemaContextRegistry) {
            return new FilterReader(consoleIO, getRemoteSchema(FilterReader.class, schemaContextRegistry));
        }
    }

    private static final class ConfigReaderProvider implements ReaderProvider {
        @Override
        public Reader<? extends DataSchemaNode> provide(final ConsoleIO consoleIO,
                final CommandArgHandlerRegistry commandArgHandlerRegistry,
                final SchemaContextRegistry schemaContextRegistry) {
            return new ConfigReader(consoleIO, getRemoteSchema(ConfigReader.class, schemaContextRegistry),
                    commandArgHandlerRegistry);
        }
    }

    private static final class EditContentReaderProvider implements ReaderProvider {
        @Override
        public Reader<? extends DataSchemaNode> provide(final ConsoleIO consoleIO,
                final CommandArgHandlerRegistry commandArgHandlerRegistry,
                final SchemaContextRegistry schemaContextRegistry) {
            return new EditContentReader(consoleIO, commandArgHandlerRegistry, getRemoteSchema(EditContentReader.class,
                    schemaContextRegistry));
        }
    }

    private static final class PasswordReaderProvider implements ReaderProvider {
        @Override
        public Reader<? extends DataSchemaNode> provide(final ConsoleIO consoleIO,
                final CommandArgHandlerRegistry commandArgHandlerRegistry,
                final SchemaContextRegistry schemaContextRegistry) {
            return new PasswordReader(consoleIO, schemaContextRegistry.getLocalSchemaContext());
        }
    }

    /**
     * Writer providers, in order to construct readers lazily
     */
    private static interface WriterProvider {
        Writer<DataSchemaNode> provide(ConsoleIO consoleIO, SchemaContext schema,
                final CommandArgHandlerRegistry commandArgHandlerRegistry);
    }

    private class DataWriterProvider implements WriterProvider {
        @Override
        public Writer<DataSchemaNode> provide(final ConsoleIO consoleIO, final SchemaContext schema,
                final CommandArgHandlerRegistry commandArgHandlerRegistry) {
            return new DataWriter(consoleIO, new OutFormatter(), schema);
        }
    }
}
