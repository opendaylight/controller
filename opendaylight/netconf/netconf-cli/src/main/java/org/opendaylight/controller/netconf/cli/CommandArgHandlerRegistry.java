/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import java.util.Map;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.Reader;
import org.opendaylight.controller.netconf.cli.reader.custom.FilterReader;
import org.opendaylight.controller.netconf.cli.reader.custom.PasswordReader;
import org.opendaylight.controller.netconf.cli.reader.impl.GenericReader;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.controller.netconf.cli.writer.impl.GenericWriter;
import org.opendaylight.controller.netconf.cli.writer.Writer;
import org.opendaylight.controller.netconf.cli.writer.custom.DataWriter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Maps;

/**
 * Keeps custom and generic input/output arguments handlers
 */
public class CommandArgHandlerRegistry {

    private final ConsoleIO consoleIO;

    private final Map<Class<? extends Reader<DataSchemaNode>>, Reader<DataSchemaNode>> customReaders = Maps.newHashMap();
    private final Map<Class<? extends Writer<DataSchemaNode>>, Writer<DataSchemaNode>> customWriters = Maps.newHashMap();
    private final Writer<DataSchemaNode> outHandler;
    private final Reader<DataSchemaNode> inHandler;

    public CommandArgHandlerRegistry(final ConsoleIO consoleIO) {
        this.consoleIO = consoleIO;
        this.outHandler = new GenericWriter(consoleIO, new OutFormatter());
        this.inHandler = new GenericReader(consoleIO);

        customReaders.put(PasswordReader.class, new PasswordReader(this.consoleIO));
    }

    public synchronized void addSchemaBasedCustomReaders(final SchemaContext remoteSchemaContext) {
        // TODO make custom readers extensible, not only from code
        customReaders.put(FilterReader.class, new FilterReader(this.consoleIO, remoteSchemaContext));
        customWriters.put(DataWriter.class, new DataWriter(this.consoleIO, new OutFormatter(), remoteSchemaContext));
    }

    public synchronized void removeSchemaBasedCustomHandlers() {
        customReaders.clear();
        customWriters.clear();
        // TODO move non schema based handlers to dedicated maps
        customReaders.put(PasswordReader.class, new PasswordReader(this.consoleIO));
    }

    public synchronized Reader<DataSchemaNode> getCustomReader(final Class<? extends Reader<DataSchemaNode>> readerType) {
        return customReaders.get(readerType);
    }

    public synchronized Reader<DataSchemaNode> getGenericReader() {
        return inHandler;
    }

    public synchronized Writer<DataSchemaNode> getCustomWriter(final Class<? extends Writer<DataSchemaNode>> readerType) {
        return customWriters.get(readerType);
    }

    public synchronized Writer<DataSchemaNode> getGenericWriter() {
        return outHandler;
    }

}
