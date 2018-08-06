/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractForwardedDataBroker implements Delegator<DOMDataBroker>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractForwardedDataBroker.class);
    // The Broker to whom we do all forwarding
    private final DOMDataBroker domDataBroker;

    private final BindingToNormalizedNodeCodec codec;

    protected AbstractForwardedDataBroker(final DOMDataBroker domDataBroker, final BindingToNormalizedNodeCodec codec,
            final DOMSchemaService schemaService) {
        this.domDataBroker = domDataBroker;
        this.codec = codec;
    }

    protected AbstractForwardedDataBroker(final DOMDataBroker domDataBroker, final BindingToNormalizedNodeCodec codec) {
        this.domDataBroker = domDataBroker;
        this.codec = codec;
    }

    protected BindingToNormalizedNodeCodec getCodec() {
        return codec;
    }

    @Override
    public DOMDataBroker getDelegate() {
        return domDataBroker;
    }

    protected Map<InstanceIdentifier<?>, DataObject> toBinding(final InstanceIdentifier<?> path,
            final Map<YangInstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized) {
        final Map<InstanceIdentifier<?>, DataObject> newMap = new HashMap<>();

        for (final Map.Entry<YangInstanceIdentifier, ? extends NormalizedNode<?, ?>> entry : normalized.entrySet()) {
            try {
                final Optional<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> potential =
                        getCodec().toBinding(entry);
                if (potential.isPresent()) {
                    final Entry<InstanceIdentifier<? extends DataObject>, DataObject> binding = potential.get();
                    newMap.put(binding.getKey(), binding.getValue());
                }
            } catch (final DeserializationException e) {
                LOG.warn("Failed to transform {}, omitting it", entry, e);
            }
        }
        return newMap;
    }

    protected Set<InstanceIdentifier<?>> toBinding(final InstanceIdentifier<?> path,
            final Set<YangInstanceIdentifier> normalized) {
        final Set<InstanceIdentifier<?>> hashSet = new HashSet<>();
        for (final YangInstanceIdentifier normalizedPath : normalized) {
            try {
                final Optional<InstanceIdentifier<? extends DataObject>> potential =
                        getCodec().toBinding(normalizedPath);
                if (potential.isPresent()) {
                    final InstanceIdentifier<? extends DataObject> binding = potential.get();
                    hashSet.add(binding);
                } else if (normalizedPath.getLastPathArgument()
                        instanceof YangInstanceIdentifier.AugmentationIdentifier) {
                    hashSet.add(path);
                }
            } catch (final DeserializationException e) {
                LOG.warn("Failed to transform {}, omitting it", normalizedPath, e);
            }
        }
        return hashSet;
    }

    protected Optional<DataObject> toBindingData(final InstanceIdentifier<?> path, final NormalizedNode<?, ?> data) {
        if (path.isWildcarded()) {
            return Optional.empty();
        }
        return (Optional<DataObject>) getCodec().deserializeFunction(path)
                .apply(Optional.<NormalizedNode<?, ?>>of(data));
    }

    @Override
    public void close() {
    }
}
