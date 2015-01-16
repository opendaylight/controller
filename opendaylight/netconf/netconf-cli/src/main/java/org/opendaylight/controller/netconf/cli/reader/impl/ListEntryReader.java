/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.netconf.cli.CommandArgHandlerRegistry;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ListEntryReader extends AbstractReader<ListSchemaNode> implements GenericListEntryReader<ListSchemaNode> {
    private static final Logger LOG = LoggerFactory.getLogger(ListEntryReader.class);

    private final CommandArgHandlerRegistry argumentHandlerRegistry;

    public ListEntryReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry,
            final SchemaContext schemaContext) {
        super(console, schemaContext);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    public ListEntryReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry,
            final SchemaContext schemaContext, final boolean readConfigNode) {
        super(console, schemaContext, readConfigNode);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    @Override
    public List<Node<?>> readWithContext(final ListSchemaNode listNode) throws IOException, ReadingException {
        console.formatLn("Submit child nodes for list entry: %s, %s", listNode.getQName().getLocalName(),
                Collections2.transform(listNode.getChildNodes(), new Function<DataSchemaNode, String>() {
                    @Override
                    public String apply(final DataSchemaNode input) {
                        return input.getQName().getLocalName();
                    }
                }));

        final String listName = listNode.getQName().getLocalName();
        final CompositeNodeBuilder<ImmutableCompositeNode> compositeNodeBuilder = ImmutableCompositeNode.builder();
        compositeNodeBuilder.setQName(listNode.getQName());

        final SeparatedNodes separatedChildNodes = SeparatedNodes.separateNodes(listNode, getReadConfigNode());

        final List<Node<?>> nodes = readKeys(separatedChildNodes.getKeyNodes());
        nodes.addAll(readMandatoryNotKeys(separatedChildNodes.getMandatoryNotKey()));
        if (!separatedChildNodes.getOthers().isEmpty()) {
            final Optional<Boolean> readNodesWhichAreNotKey = new DecisionReader().read(console,
                    "Add non-key, non-mandatory nodes to list %s? [Y|N]", listName);
            if (readNodesWhichAreNotKey.isPresent() && readNodesWhichAreNotKey.get()) {
                nodes.addAll(readNotKeys(separatedChildNodes.getOthers()));
            }
        }

        if (!nodes.isEmpty()) {
            compositeNodeBuilder.addAll(nodes);
            return Collections.<Node<?>> singletonList(compositeNodeBuilder.build());
        } else {
            return Collections.emptyList();
        }
    }

    private List<Node<?>> readKeys(final Set<DataSchemaNode> keys) throws ReadingException, IOException {
        final List<Node<?>> newNodes = new ArrayList<>();
        console.writeLn("Reading keys:");
        for (final DataSchemaNode key : keys) {
            final List<Node<?>> readKey = new LeafReader(console, getSchemaContext(), getReadConfigNode())
                    .read((LeafSchemaNode) key);
            if (readKey.size() != 1) {
                final String message = String.format(
                        "Value for key element %s has to be set. Creation of this entry is canceled.", key.getQName()
                                .getLocalName());
                LOG.error(message);
                throw new ReadingException(message);
            }
            newNodes.addAll(readKey);
        }
        return newNodes;
    }

    private List<Node<?>> readMandatoryNotKeys(final Set<DataSchemaNode> mandatoryNotKeys) throws ReadingException,
            IOException {
        final List<Node<?>> newNodes = new ArrayList<>();
        console.writeLn("Reading mandatory not keys nodes:");

        for (final DataSchemaNode mandatoryNode : mandatoryNotKeys) {
            final List<Node<?>> redValue = argumentHandlerRegistry.getGenericReader(getSchemaContext(),
                    getReadConfigNode()).read(mandatoryNode);
            if (redValue.isEmpty()) {
                final String message = String.format(
                        "Value for mandatory element %s has to be set. Creation of this entry is canceled.",
                        mandatoryNode.getQName().getLocalName());
                LOG.error(message);
                throw new ReadingException(message);
            }
            newNodes.addAll(redValue);
        }
        return newNodes;
    }

    private List<Node<?>> readNotKeys(final Set<DataSchemaNode> notKeys) throws ReadingException {
        final List<Node<?>> newNodes = new ArrayList<>();
        for (final DataSchemaNode notKey : notKeys) {
            newNodes.addAll(argumentHandlerRegistry.getGenericReader(getSchemaContext(), getReadConfigNode()).read(
                    notKey));
        }
        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final ListSchemaNode schemaNode) {
        return new BaseConsoleContext<ListSchemaNode>(schemaNode) {
            @Override
            public Optional<String> getPrompt() {
                return Optional.of("[entry]");
            }
        };
    }

}
