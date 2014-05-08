/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.custom;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.isSkipInput;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import jline.console.completer.Completer;
import org.opendaylight.controller.netconf.cli.CommandArgHandlerRegistry;
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
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Custom reader implementation for filter elements in get/get-config rpcs. This
 * reader overrides the default anyxml reader and reads filter as a schema path.
 */
public class ConfigReader extends AbstractReader<DataSchemaNode> {

    public static final String SEPARATOR = "/";

    private final CommandArgHandlerRegistry commandArgHandlerRegistry;
    private final Map<String, QName> mappedModules;
    private final Map<URI, QName> mappedModulesNamespace;

    public ConfigReader(final ConsoleIO console, final SchemaContext remoteSchemaContext,
            final CommandArgHandlerRegistry commandArgHandlerRegistry) {
        super(console, remoteSchemaContext);
        this.commandArgHandlerRegistry = commandArgHandlerRegistry;

        mappedModules = Maps.newHashMap();
        mappedModulesNamespace = Maps.newHashMap();
        for (final Module module : remoteSchemaContext.getModules()) {
            final QName moduleQName = QName.create(module.getNamespace(), module.getRevision(), module.getName());
            mappedModules.put(moduleQName.getLocalName(), moduleQName);
            mappedModulesNamespace.put(moduleQName.getNamespace(), moduleQName);
        }
    }

    // FIXME refactor + unite common code with FilterReader

    @Override
    protected List<Node<?>> readWithContext(final DataSchemaNode schemaNode) throws IOException, ReadingException {
        console.writeLn("Config " + schemaNode.getQName().getLocalName());
        console.writeLn("Submit path of the data to edit. Use TAB for autocomplete");

        final String rawValue = console.read();

        // FIXME isSkip check should be somewhere in abstractReader
        if (isSkipInput(rawValue) || Strings.isNullOrEmpty(rawValue)) {
            return Collections.emptyList();
        }

        final List<QName> filterPartsQNames = Lists.newArrayList();

        for (final String part : rawValue.split(SEPARATOR)) {
            final QName qName = IOUtil.qNameFromKeyString(part, mappedModules);
            filterPartsQNames.add(qName);
        }

        List<Node<?>> previous = readInnerNode(rawValue);

        for (final QName qName : Lists.reverse(filterPartsQNames).subList(1, filterPartsQNames.size())) {
            previous = Collections.<Node<?>> singletonList(new CompositeNodeTOImpl(qName, null,
                    previous == null ? Collections.<Node<?>> emptyList() : previous));
        }

        final Node<?> newNode = previous == null ? null
                : new CompositeNodeTOImpl(schemaNode.getQName(), null, previous);

        return Collections.<Node<?>> singletonList(newNode);
    }

    private List<Node<?>> readInnerNode(final String pathString) throws ReadingException {
        final Optional<DataSchemaNode> schema = getCurrentNode(getSchemaContext(), pathString);
        Preconditions.checkState(schema.isPresent(), "Unable to find schema for %s", pathString);
        return commandArgHandlerRegistry.getGenericReader(getSchemaContext(), true).read(schema.get());
    }

    @Override
    protected ConsoleContext getContext(final DataSchemaNode schemaNode) {
        return new FilterConsoleContext(schemaNode, getSchemaContext());
    }

    private final class FilterConsoleContext extends BaseConsoleContext<DataSchemaNode> {

        private final SchemaContext remoteSchemaContext;

        public FilterConsoleContext(final DataSchemaNode schemaNode, final SchemaContext remoteSchemaContext) {
            super(schemaNode);
            this.remoteSchemaContext = remoteSchemaContext;
        }

        @Override
        protected List<Completer> getAdditionalCompleters() {
            return Collections.<Completer> singletonList(new FilterCompleter(remoteSchemaContext));
        }
    }

    private final class FilterCompleter implements Completer {

        private final SchemaContext remoteSchemaContext;

        public FilterCompleter(final SchemaContext remoteSchemaContext) {
            this.remoteSchemaContext = remoteSchemaContext;
        }

        @Override
        public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
            final int idx = buffer.lastIndexOf(SEPARATOR);

            final Optional<DataSchemaNode> currentNode = getCurrentNode(remoteSchemaContext, buffer);
            if (currentNode.isPresent() && currentNode.get() instanceof DataNodeContainer) {
                final Collection<DataSchemaNode> childNodes = ((DataNodeContainer) currentNode.get()).getChildNodes();
                final Collection<String> transformed = Collections2.transform(childNodes,
                        new Function<DataSchemaNode, String>() {
                            @Override
                            public String apply(final DataSchemaNode input) {
                                return IOUtil.qNameToKeyString(input.getQName(),
                                        mappedModulesNamespace.get(input.getQName().getNamespace()).getLocalName());
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

    }

    private Optional<DataSchemaNode> getCurrentNode(DataSchemaNode parent, final String buffer) {
        for (final String part : buffer.split(SEPARATOR)) {
            if (IOUtil.isQName(part) == false) {
                return Optional.of(parent);
            }

            final QName qName;
            try {
                qName = IOUtil.qNameFromKeyString(part, mappedModules);
            } catch (final ReadingException e) {
                return Optional.of(parent);
            }
            if (parent instanceof DataNodeContainer) {
                parent = ((DataNodeContainer) parent).getDataChildByName(qName);
            } else {
                // This should check if we are at the end of buffer ?
                return Optional.of(parent);
            }
        }
        return Optional.of(parent);
    }

}
