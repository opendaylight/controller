/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static com.google.common.base.Verify.verify;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.Uint16TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint32TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint64TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint8TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UintAdaptingPruner extends ReusableNormalizedNodePruner {
    @FunctionalInterface
    private interface NipAdapter extends Function<NodeIdentifierWithPredicates, NodeIdentifierWithPredicates> {

    }

    private enum ValueAdapter implements Function<Object, Object> {
        UINT8 {
            @Override
            public Object apply(final Object obj) {
                if (obj instanceof Short) {
                    LOG.trace("Translating legacy uint8 {}", obj);
                    return Uint8.valueOf((Short) obj);
                }
                return obj;
            }
        },
        UINT16 {
            @Override
            public Object apply(final Object obj) {
                if (obj instanceof Integer) {
                    LOG.trace("Translating legacy uint16 {}", obj);
                    return Uint16.valueOf((Integer) obj);
                }
                return obj;
            }
        },
        UINT32 {
            @Override
            public Object apply(final Object obj) {
                if (obj instanceof Long) {
                    LOG.trace("Translating legacy uint32 {}", obj);
                    return Uint32.valueOf((Long) obj);
                }
                return obj;
            }
        },
        UINT64 {
            @Override
            public Object apply(final Object obj) {
                if (obj instanceof BigInteger) {
                    LOG.trace("Translating legacy uint64 {}", obj);
                    return Uint64.valueOf((BigInteger) obj);
                }
                return obj;
            }
        };

        private static final Logger LOG = LoggerFactory.getLogger(ValueAdapter.class);

        static @Nullable ValueAdapter forType(final TypeDefinition<?> type) {
            if (type instanceof Uint8TypeDefinition) {
                return UINT8;
            } else if (type instanceof Uint16TypeDefinition) {
                return UINT16;
            } else if (type instanceof Uint32TypeDefinition) {
                return UINT32;
            } else if (type instanceof Uint64TypeDefinition) {
                return UINT64;
            } else {
                return null;
            }
        }
    }

    private static final LoadingCache<ListSchemaNode, NipAdapter> NIP_ADAPTERS = CacheBuilder.newBuilder()
            .weakKeys().build(new AdapterCacheLoader());

    UintAdaptingPruner(final DataSchemaContextTree tree) {
        super(tree);
    }

    @Override
    public ReusableNormalizedNodePruner duplicate() {
        return new UintAdaptingPruner(getTree());
    }

    @Override
    public void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint)
            throws IOException {
        enter(this::adaptEntry, identifier, childSizeHint);
    }

    @Override
    public void startLeafSetEntryNode(final NodeWithValue<?> name) throws IOException {
        enter(this::adaptEntry, name);
    }

    @Override
    Object translateScalar(final DataSchemaContextNode<?> context, final Object value) throws IOException {
        final DataSchemaNode schema = context.getDataSchemaNode();
        return schema instanceof TypedDataSchemaNode ? adaptValue(((TypedDataSchemaNode) schema).getType(), value)
                : value;
    }

    private void adaptEntry(final ReusableImmutableNormalizedNodeStreamWriter writer, final NodeWithValue<?> name) {
        final NodeWithValue<?> adapted;
        final DataSchemaNode schema = currentSchema().getDataSchemaNode();
        if (schema instanceof TypedDataSchemaNode) {
            final Object oldValue = name.getValue();
            final Object newValue = adaptValue(((TypedDataSchemaNode) schema).getType(), oldValue);
            adapted = newValue == oldValue ? name : new NodeWithValue<>(name.getNodeType(), newValue);
        } else {
            adapted = name;
        }

        writer.startLeafSetEntryNode(adapted);
    }

    private void adaptEntry(final ReusableImmutableNormalizedNodeStreamWriter writer,
            final NodeIdentifierWithPredicates name, final int size) {
        final NodeIdentifierWithPredicates adapted;
        final DataSchemaNode schema = currentSchema().getDataSchemaNode();
        if (schema instanceof ListSchemaNode) {
            adapted = NIP_ADAPTERS.getUnchecked((ListSchemaNode) schema).apply(name);
        } else {
            adapted = name;
        }

        writer.startMapEntryNode(adapted, size);
    }

    private static Object adaptValue(final TypeDefinition<?> type, final Object value) {
        final ValueAdapter adapter = ValueAdapter.forType(type);
        return adapter != null ? adapter.apply(value) : value;
    }

    private static final class AdapterCacheLoader extends CacheLoader<ListSchemaNode, NipAdapter> {
        @Override
        public NipAdapter load(final ListSchemaNode key) {
            final Map<QName, ValueAdapter> adapters = new HashMap<>();

            for (QName qname : key.getKeyDefinition()) {
                final DataSchemaNode child;
                try {
                    child = key.findDataTreeChild(qname).orElseThrow();
                } catch (NoSuchElementException e) {
                    throw new IllegalStateException("Failed to find child " + qname, e);
                }

                verify(child instanceof LeafSchemaNode, "Key references non-leaf child %s", child);
                final ValueAdapter adapter = ValueAdapter.forType(((LeafSchemaNode) child).getType());
                if (adapter != null) {
                    adapters.put(qname, adapter);
                }
            }

            return adapters.isEmpty() ? name -> name : new TransformingNipAdapter(adapters);
        }
    }

    private static final class TransformingNipAdapter implements NipAdapter {
        private final ImmutableMap<QName, ValueAdapter> adapters;

        TransformingNipAdapter(final Map<QName, ValueAdapter> toTransform) {
            adapters = ImmutableMap.copyOf(toTransform);
        }

        @Override
        public NodeIdentifierWithPredicates apply(final NodeIdentifierWithPredicates name) {
            final Set<Entry<QName, Object>> entries = name.entrySet();
            final ImmutableMap.Builder<QName, Object> newEntries = ImmutableMap.builderWithExpectedSize(entries.size());
            for (Entry<QName, Object> e : entries) {
                final QName qname = e.getKey();
                final ValueAdapter adapter = adapters.get(qname);
                newEntries.put(qname, adapter != null ? adapter.apply(e.getValue()) : e.getValue());
            }

            return NodeIdentifierWithPredicates.of(name.getNodeType(), newEntries.build());
        }
    }
}
