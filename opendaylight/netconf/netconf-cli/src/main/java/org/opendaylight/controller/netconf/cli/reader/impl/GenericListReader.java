/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.listType;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericListReader<T extends DataSchemaNode> extends AbstractReader<T> {
    private static final Logger LOG = LoggerFactory.getLogger(GenericListReader.class);

    private final GenericListEntryReader<T> concreteListEntryReader;

    public GenericListReader(final ConsoleIO console, final GenericListEntryReader<T> concreteListEntryReader,
            final SchemaContext schemaContext) {
        super(console, schemaContext);
        this.concreteListEntryReader = concreteListEntryReader;
    }

    public GenericListReader(final ConsoleIO console, final GenericListEntryReader<T> concreteListEntryReader,
            final SchemaContext schemaContext, final boolean readConfigNode) {
        super(console, schemaContext, readConfigNode);
        this.concreteListEntryReader = concreteListEntryReader;
    }

    @Override
    public List<Node<?>> readWithContext(final T schemaNode) throws IOException, ReadingException {
        final List<Node<?>> newNodes = new ArrayList<>();
        Optional<Boolean> readNextListEntry = Optional.of(Boolean.TRUE);
        console.formatLn("Reading collection type argument: %s", schemaNode.getQName().getLocalName());
        while (readNextListEntry.isPresent() && readNextListEntry.get()) {
            try {
                newNodes.addAll(concreteListEntryReader.read(schemaNode));
            } catch (final ReadingException e) {
                console.writeLn(e.getMessage());
            }
            readNextListEntry = new DecisionReader().read(console, "Add other entry to " + listType(schemaNode) + " "
                    + schemaNode.getQName().getLocalName() + " " + " [Y|N]?");
        }
        console.formatLn("Collection type argument: %s read finished", schemaNode.getQName().getLocalName());

        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final T schemaNode) {
        return new BaseConsoleContext<>(schemaNode);
    }

}
