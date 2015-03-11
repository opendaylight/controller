/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.isSkipInput;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.opendaylight.controller.netconf.cli.CommandArgHandlerRegistry;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChoiceReader extends AbstractReader<ChoiceSchemaNode> {

    private static final Logger LOG = LoggerFactory.getLogger(ChoiceReader.class);

    private final CommandArgHandlerRegistry argumentHandlerRegistry;

    public ChoiceReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry,
            final SchemaContext schemaContext) {
        super(console, schemaContext);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    public ChoiceReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry,
            final SchemaContext schemaContext, final boolean readConfigNode) {
        super(console, schemaContext, readConfigNode);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    @Override
    public List<Node<?>> readWithContext(final ChoiceSchemaNode choiceNode) throws IOException, ReadingException {
        final Map<String, ChoiceCaseNode> availableCases = collectAllCases(choiceNode);
        console.formatLn("Select case for choice %s from: %s", choiceNode.getQName().getLocalName(),
                formatSet(availableCases.keySet()));

        ChoiceCaseNode selectedCase = null;
        final String rawValue = console.read();
        if (isSkipInput(rawValue)) {
            return Collections.emptyList();
        }

        selectedCase = availableCases.get(rawValue);
        if (selectedCase == null) {
            final String message = String.format("Incorrect value (%s) for choice %s was selected.", rawValue,
                    choiceNode.getQName().getLocalName());
            LOG.error(message);
            throw new ReadingException(message);
        }

        return readSelectedCase(selectedCase);
    }

    protected List<Node<?>> readSelectedCase(final ChoiceCaseNode selectedCase) throws ReadingException {
        // IF there is a case that contains only one Empty type leaf, create the
        // leaf without question, since the case was selected
        if (containsOnlyOneEmptyLeaf(selectedCase)) {
            final Node<?> newNode = NodeFactory.createImmutableSimpleNode(selectedCase.getChildNodes().iterator()
                    .next().getQName(), null, null);
            return Collections.<Node<?>> singletonList(newNode);
        }

        final List<Node<?>> newNodes = new ArrayList<>();
        for (final DataSchemaNode schemaNode : selectedCase.getChildNodes()) {
            newNodes.addAll(argumentHandlerRegistry.getGenericReader(getSchemaContext(), getReadConfigNode()).read(
                    schemaNode));
        }
        return newNodes;
    }

    private Object formatSet(final Set<String> values) {
        final StringBuilder formatedValues = new StringBuilder();
        for (final String value : values) {
            formatedValues.append("\n  ");
            formatedValues.append(value);
        }
        return formatedValues.toString();
    }

    private boolean containsOnlyOneEmptyLeaf(final ChoiceCaseNode selectedCase) {
        if (selectedCase.getChildNodes().size() != 1) {
            return false;
        }
        final DataSchemaNode next = selectedCase.getChildNodes().iterator().next();
        if (next instanceof LeafSchemaNode) {
            final TypeDefinition<?> type = ((LeafSchemaNode) next).getType();
            if (isEmptyType(type)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, ChoiceCaseNode> collectAllCases(final ChoiceSchemaNode schemaNode) {
        return Maps.uniqueIndex(schemaNode.getCases(), new Function<ChoiceCaseNode, String>() {
            @Override
            public String apply(final ChoiceCaseNode input) {
                return input.getQName().getLocalName();
            }
        });
    }

    @Override
    protected ConsoleContext getContext(final ChoiceSchemaNode schemaNode) {
        return new BaseConsoleContext<ChoiceSchemaNode>(schemaNode) {
            @Override
            public List<Completer> getAdditionalCompleters() {
                return Collections
                        .<Completer> singletonList(new StringsCompleter(collectAllCases(schemaNode).keySet()));
            }
        };
    }
}
