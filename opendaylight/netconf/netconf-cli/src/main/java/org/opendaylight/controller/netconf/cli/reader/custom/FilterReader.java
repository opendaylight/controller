/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.custom;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.listType;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.qNameToKeyString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import jline.console.completer.Completer;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

public class FilterReader extends AbstractReader<DataSchemaNode> {

    public static final String SEPARATOR = "#";
    private final SchemaContext remoteSchemaContext;

    public FilterReader(final ConsoleIO console, final SchemaContext remoteSchemaContext) {
        super(console);
        this.remoteSchemaContext = remoteSchemaContext;
    }

    // FIXME refactor

    @Override
    protected List<Node<?>> readInner(final DataSchemaNode schemaNode) throws IOException, ReadingException {
        console.writeLn(listType(schemaNode) + " " + schemaNode.getQName().getLocalName());

        final String rawValue = console.read();

        Node<?> newNode = null;
        // FIXME skip should be somewhere in abstractReader
        if (!rawValue.equals(SKIP)) {

            List<QName> filterPartsQNames = Lists.newArrayList();

            for (final String part : rawValue.split(SEPARATOR)) {
                final QName qName = IOUtil.qNameFromKeyString(part);
                filterPartsQNames.add(qName);
            }

            Node<?> previous = null;

            for (QName qName : Lists.reverse(filterPartsQNames)) {
                previous = new CompositeNodeTOImpl(qName, null, previous == null ? Collections.<Node<?>> emptyList()
                        : Collections.<Node<?>> singletonList(previous));
            }

            newNode = previous == null ? null : new CompositeNodeTOImpl(schemaNode.getQName(), null,
                    Collections.<Node<?>> singletonList(previous));
        }

        final List<Node<?>> newNodes = new ArrayList<>();
        newNodes.add(newNode);
        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final DataSchemaNode schemaNode) {
        return new FilterConsoleContext(schemaNode, remoteSchemaContext);
    }

    private static class FilterConsoleContext extends BaseConsoleContext {

        private final SchemaContext remoteSchemaContext;

        public FilterConsoleContext(final DataSchemaNode schemaNode, final SchemaContext remoteSchemaContext) {
            super(schemaNode);
            this.remoteSchemaContext = remoteSchemaContext;
        }

        @Override
        public Completer getCompleter() {
            return new FilterCompleter(remoteSchemaContext);
        }

    }

    private static class FilterCompleter implements Completer {

        private final SchemaContext remoteSchemaContext;

        public FilterCompleter(final SchemaContext remoteSchemaContext) {
            this.remoteSchemaContext = remoteSchemaContext;
        }

        @Override
        public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
            final int idx = buffer.lastIndexOf(SEPARATOR);

            final Optional<DataNodeContainer> currentNode = getCurrentNode(remoteSchemaContext, buffer);
            if (currentNode.isPresent()) {

                final Collection<String> transformed = Collections2.transform(currentNode.get().getChildNodes(),
                        new Function<DataSchemaNode, String>() {
                            @Override
                            public String apply(final DataSchemaNode input) {
                                return qNameToKeyString(input.getQName());
                            }
                        });

                fillCandidates(buffer.substring(idx + 1), candidates, transformed);
            }

            return idx == -1 ? 0 : idx + 1;
        }

        private void fillCandidates(final String buffer, final List<CharSequence> candidates,
                final Collection<String> transformed) {
            final SortedSet<String> strings = new TreeSet<>(transformed);
            if (buffer == null) {
                candidates.addAll(strings);
            } else {
                for (final String match : strings.tailSet(buffer)) {
                    if (!match.startsWith(buffer)) {
                        break;
                    }
                    candidates.add(match);
                }
            }

            if (candidates.size() == 1) {
                candidates.set(0, candidates.get(0) + SEPARATOR);
            }
        }

        private Optional<DataNodeContainer> getCurrentNode(DataNodeContainer parent, final String buffer) {
            for (final String part : buffer.split(SEPARATOR)) {
                if (IOUtil.isQName(part) == false) {
                    return Optional.of(parent);
                }

                final QName qName = IOUtil.qNameFromKeyString(part);
                final DataSchemaNode dataChildByName = parent.getDataChildByName(qName);
                if (dataChildByName instanceof DataNodeContainer) {
                    parent = (DataNodeContainer) dataChildByName;
                } else {
                    return Optional.absent();
                }
            }
            return Optional.of(parent);
        }
    }

}
