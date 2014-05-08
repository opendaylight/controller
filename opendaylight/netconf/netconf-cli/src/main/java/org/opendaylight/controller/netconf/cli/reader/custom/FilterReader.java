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
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom reader implementation for filter elements in get/get-config rpcs. This
 * reader overrides the default anyxml reader and reads filter as a schema path.
 */
public class FilterReader extends AbstractReader<DataSchemaNode> {

    private static final Logger LOG = LoggerFactory.getLogger(FilterReader.class);

    public static final String SEPARATOR = "/";

    private final Map<String, QName> mappedModules;
    private final Map<URI, QName> mappedModulesNamespace;

    public FilterReader(final ConsoleIO console, final SchemaContext remoteSchemaContext) {
        super(console, remoteSchemaContext);

        mappedModules = Maps.newHashMap();
        mappedModulesNamespace = Maps.newHashMap();
        for (final Module module : remoteSchemaContext.getModules()) {
            final QName moduleQName = QName.create(module.getNamespace(), module.getRevision(), module.getName());
            mappedModules.put(moduleQName.getLocalName(), moduleQName);
            mappedModulesNamespace.put(moduleQName.getNamespace(), moduleQName);
        }
    }

    // FIXME refactor

    public static final QName FILTER_TYPE_QNAME = QName.create("urn:ietf:params:xml:ns:netconf:base:1.0", "2011-06-01",
            "type");
    public static final String FILTER_TYPE_VALUE_DEFAULT = "subtree";

    @Override
    protected List<Node<?>> readWithContext(final DataSchemaNode schemaNode) throws IOException, ReadingException {
        boolean redSuccessfuly = false;
        Node<?> newNode = null;
        do {
            console.writeLn("Filter " + schemaNode.getQName().getLocalName());
            console.writeLn("Submit path of the data to retrieve. Use TAB for autocomplete");

            final String rawValue = console.read();

            // FIXME skip should be somewhere in abstractReader
            if (isSkipInput(rawValue) || Strings.isNullOrEmpty(rawValue)) {
                return Collections.emptyList();
            }

            final List<QName> filterPartsQNames = Lists.newArrayList();

            try {
                for (final String part : rawValue.split(SEPARATOR)) {
                    final QName qName = IOUtil.qNameFromKeyString(part, mappedModules);
                    filterPartsQNames.add(qName);
                }

                Node<?> previous = null;

                for (final QName qName : Lists.reverse(filterPartsQNames)) {
                    previous = new CompositeNodeTOImpl(qName, null,
                            previous == null ? Collections.<Node<?>> emptyList()
                                    : Collections.<Node<?>> singletonList(previous));
                }

                final Map<QName, String> attributes = Collections.singletonMap(FILTER_TYPE_QNAME,
                        FILTER_TYPE_VALUE_DEFAULT);
                newNode = previous == null ? null : ImmutableCompositeNode.create(schemaNode.getQName(), attributes,
                        Collections.<Node<?>> singletonList(previous));
                redSuccessfuly = true;
            } catch (final ReadingException e) {
                final String message = "Specified filter path isn't correct.";
                LOG.error(message, e);
                console.writeLn(message);
            }
        } while (!redSuccessfuly);
        return Collections.<Node<?>> singletonList(newNode);
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

        // TODO add skip to filter completer, better soulution would be to add
        // SKIP completer before context completer if possible

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

        private Optional<DataNodeContainer> getCurrentNode(DataNodeContainer parent, final String buffer) {
            for (final String part : buffer.split(SEPARATOR)) {
                if (!IOUtil.isQName(part)) {
                    return Optional.of(parent);
                }

                QName qName;
                try {
                    qName = IOUtil.qNameFromKeyString(part, mappedModules);
                } catch (final ReadingException e) {
                    return Optional.of(parent);
                }

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
