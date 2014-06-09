/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.netconf.cli.CommandArgHandlerRegistry;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class ContainerReader extends AbstractReader<ContainerSchemaNode> {

    private final CommandArgHandlerRegistry argumentHandlerRegistry;

    public ContainerReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry) {
        super(console);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    @Override
    public List<Node<?>> readWithContext(final ContainerSchemaNode containerNode) throws IOException, ReadingException {
        final CompositeNodeBuilder<ImmutableCompositeNode> compositeNodeBuilder = ImmutableCompositeNode.builder();
        compositeNodeBuilder.setQName(containerNode.getQName());
        for (final DataSchemaNode childNode : containerNode.getChildNodes()) {
            compositeNodeBuilder.addAll(argumentHandlerRegistry.getGenericReader().read(childNode));
        }
        return Collections.<Node<?>> singletonList(compositeNodeBuilder.toInstance());
    }

    @Override
    protected ConsoleContext getContext(final ContainerSchemaNode schemaNode) {
        return new BaseConsoleContext<>(schemaNode);
    }
}
