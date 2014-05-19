/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl.compat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.collect.Maps;

public abstract class TranslatingDataChangeEvent implements
        DataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> {

    private TranslatingDataChangeEvent() {
    }

    public static DataChangeEvent<InstanceIdentifier, CompositeNode> createOperational(
            final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change, final DataNormalizer normalizer) {
        return new OperationalChangeEvent(change, normalizer);
    }

    public static DataChangeEvent<InstanceIdentifier, CompositeNode> createConfiguration(
            final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change, final DataNormalizer normalizer) {
        return new ConfigurationChangeEvent(change, normalizer);
    }

    @Override
    public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getCreatedOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getCreatedConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getUpdatedOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getUpdatedConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Set<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> getRemovedConfigurationData() {
        return Collections.emptySet();
    }

    @Override
    public Set<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> getRemovedOperationalData() {
        return Collections.emptySet();
    }

    @Override
    public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getOriginalConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getOriginalOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public CompositeNode getOriginalConfigurationSubtree() {
        return null;
    }

    @Override
    public CompositeNode getOriginalOperationalSubtree() {
        return null;
    }

    @Override
    public CompositeNode getUpdatedConfigurationSubtree() {
        return null;
    }

    @Override
    public CompositeNode getUpdatedOperationalSubtree() {
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private final static class OperationalChangeEvent extends TranslatingDataChangeEvent {

        private final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> delegate;
        private final DataNormalizer normalizer;
        private Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> updatedCache;

        public OperationalChangeEvent(final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change,
                                        final DataNormalizer normalizer) {
            this.delegate = change;
            this.normalizer = normalizer;
        }

        @Override
        public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getCreatedOperationalData() {
            return transformToLegacy(normalizer, delegate.getCreatedData());
        }


        @Override
        public Set<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> getRemovedOperationalData() {
            return delegate.getRemovedPaths();
        }

        @Override
        public CompositeNode getOriginalOperationalSubtree() {
            // first argument is unused
            return normalizer.toLegacy(null, delegate.getOriginalSubtree());
        }

        @Override
        public CompositeNode getUpdatedOperationalSubtree() {
            // first argument is unused
            return normalizer.toLegacy(null, delegate.getUpdatedSubtree());
        }

        @Override
        public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getOriginalOperationalData() {
            return transformToLegacy(normalizer, delegate.getOriginalData());
        }

        @Override
        public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getUpdatedOperationalData() {
            if(updatedCache == null) {
                final Map<InstanceIdentifier, CompositeNode> updated = transformToLegacy(normalizer, delegate.getUpdatedData());
                final Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> created = getCreatedConfigurationData();
                final HashMap<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> updatedComposite = new HashMap<>(created.size() + updated.size());
                updatedComposite.putAll(created);
                updatedComposite.putAll(updated);
                updatedCache = Collections.unmodifiableMap(updatedComposite);
            }
            return updatedCache;
        }

        @Override
        public String toString() {
            return "OperationalChangeEvent [delegate=" + delegate + "]";
        }

    }

    private static Map<InstanceIdentifier, CompositeNode> transformToLegacy(final DataNormalizer normalizer, final Map<InstanceIdentifier, ? extends NormalizedNode<?, ?>> nodes) {
        final Map<InstanceIdentifier, CompositeNode> legacy = Maps.newHashMap();

        for (final Map.Entry<InstanceIdentifier, ? extends NormalizedNode<?, ?>> entry : nodes.entrySet()) {
            try {
                legacy.put(normalizer.toLegacy(entry.getKey()), normalizer.toLegacy(entry.getKey(), entry.getValue()));
            } catch (final DataNormalizationException e) {
                throw new IllegalStateException("Unable to transform data change event to legacy format", e);
            }
        }
        return legacy;
    }

    private final static class ConfigurationChangeEvent extends TranslatingDataChangeEvent {

        private final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> delegate;
        private final DataNormalizer normalizer;
        private Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> updatedCache;

        public ConfigurationChangeEvent(final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change,
                                        final DataNormalizer normalizer) {
            this.delegate = change;
            this.normalizer = normalizer;
        }

        @Override
        public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getCreatedConfigurationData() {
            return transformToLegacy(normalizer, delegate.getCreatedData());
        }


        @Override
        public Set<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> getRemovedConfigurationData() {
            return delegate.getRemovedPaths();
        }

        @Override
        public CompositeNode getOriginalConfigurationSubtree() {
            // first argument is unused
            return normalizer.toLegacy(null, delegate.getOriginalSubtree());
        }

        @Override
        public CompositeNode getUpdatedConfigurationSubtree() {
            // first argument is unused
            return normalizer.toLegacy(null, delegate.getUpdatedSubtree());
        }

        @Override
        public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getOriginalConfigurationData() {
            return transformToLegacy(normalizer, delegate.getOriginalData());
        }

        @Override
        public Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getUpdatedConfigurationData() {
            if(updatedCache == null) {
                final Map<InstanceIdentifier, CompositeNode> updated = transformToLegacy(normalizer, delegate.getUpdatedData());
                final Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> created = getCreatedConfigurationData();
                final HashMap<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> updatedComposite = new HashMap<>(created.size() + updated.size());
                updatedComposite.putAll(created);
                updatedComposite.putAll(updated);
                updatedCache = Collections.unmodifiableMap(updatedComposite);
            }
            return updatedCache;
        }

        @Override
        public String toString() {
            return "ConfigurationChangeEvent [delegate=" + delegate + "]";
        }
    }
}
