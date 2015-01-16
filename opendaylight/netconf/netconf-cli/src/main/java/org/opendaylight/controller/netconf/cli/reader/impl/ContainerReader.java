/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ContainerReader extends AbstractReader<ContainerSchemaNode> {

    private final CommandArgHandlerRegistry argumentHandlerRegistry;
    private static final InputArgsLocalNameComparator CONTAINER_CHILDS_SORTER = new InputArgsLocalNameComparator();

    public ContainerReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry,
            final SchemaContext schemaContext) {
        super(console, schemaContext);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    public ContainerReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry,
            final SchemaContext schemaContext, final boolean readConfigNode) {
        super(console, schemaContext, readConfigNode);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    @Override
    public List<Node<?>> readWithContext(final ContainerSchemaNode containerNode) throws IOException, ReadingException {
        console.formatLn("Submit child nodes for container: %s, %s", containerNode.getQName().getLocalName(),
                Collections2.transform(containerNode.getChildNodes(), new Function<DataSchemaNode, String>() {
                    @Override
                    public String apply(final DataSchemaNode input) {
                        return input.getQName().getLocalName();
                    }
                }));

        final CompositeNodeBuilder<ImmutableCompositeNode> compositeNodeBuilder = ImmutableCompositeNode.builder();
        compositeNodeBuilder.setQName(containerNode.getQName());
        final SeparatedNodes separatedNodes = SeparatedNodes.separateNodes(containerNode, getReadConfigNode());
        for (final DataSchemaNode childNode : sortChildren(separatedNodes.getMandatoryNotKey())) {
            final List<Node<?>> redNodes = argumentHandlerRegistry.getGenericReader(getSchemaContext(),
                    getReadConfigNode()).read(childNode);
            if (redNodes.isEmpty()) {
                console.formatLn("No data specified for mandatory element %s.", childNode.getQName().getLocalName());
                return Collections.emptyList();
            } else {
                compositeNodeBuilder.addAll(redNodes);
            }
        }

        for (final DataSchemaNode childNode : sortChildren(separatedNodes.getOthers())) {
            compositeNodeBuilder.addAll(argumentHandlerRegistry.getGenericReader(getSchemaContext(),
                    getReadConfigNode()).read(childNode));
        }
        return Collections.<Node<?>> singletonList(compositeNodeBuilder.build());
    }

    private List<DataSchemaNode> sortChildren(final Set<DataSchemaNode> unsortedNodes) {
        final List<DataSchemaNode> childNodes = Lists.newArrayList(unsortedNodes);
        Collections.sort(childNodes, CONTAINER_CHILDS_SORTER);
        return childNodes;
    }

    @Override
    protected ConsoleContext getContext(final ContainerSchemaNode schemaNode) {
        return new BaseConsoleContext<>(schemaNode);
    }

    private static class InputArgsLocalNameComparator implements Comparator<DataSchemaNode> {
        @Override
        public int compare(final DataSchemaNode o1, final DataSchemaNode o2) {
            return o1.getQName().getLocalName().compareTo(o2.getQName().getLocalName());
        }
    }

}
