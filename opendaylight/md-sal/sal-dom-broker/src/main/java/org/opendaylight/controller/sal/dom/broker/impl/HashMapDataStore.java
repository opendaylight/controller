/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HashMapDataStore implements DataStore, AutoCloseable {
    private static final Logger LOG = LoggerFactory
            .getLogger(HashMapDataStore.class);

    private final Map<YangInstanceIdentifier, CompositeNode> configuration = new ConcurrentHashMap<YangInstanceIdentifier, CompositeNode>();
    private final Map<YangInstanceIdentifier, CompositeNode> operational = new ConcurrentHashMap<YangInstanceIdentifier, CompositeNode>();

    @Override
    public boolean containsConfigurationPath(final YangInstanceIdentifier path) {
        return configuration.containsKey(path);
    }

    @Override
    public boolean containsOperationalPath(final YangInstanceIdentifier path) {
        return operational.containsKey(path);
    }

    @Override
    public Iterable<YangInstanceIdentifier> getStoredConfigurationPaths() {
        return configuration.keySet();
    }

    @Override
    public Iterable<YangInstanceIdentifier> getStoredOperationalPaths() {
        return operational.keySet();
    }

    @Override
    public CompositeNode readConfigurationData(final YangInstanceIdentifier path) {
        LOG.trace("Reading configuration path {}", path);
        return configuration.get(path);
    }

    @Override
    public CompositeNode readOperationalData(YangInstanceIdentifier path) {
        LOG.trace("Reading operational path {}", path);
        return operational.get(path);
    }

    @Override
    public DataCommitHandler.DataCommitTransaction<YangInstanceIdentifier, CompositeNode> requestCommit(
            final DataModification<YangInstanceIdentifier, CompositeNode> modification) {
        return new HashMapDataStoreTransaction(modification, this);
    }

    public RpcResult<Void> rollback(HashMapDataStoreTransaction transaction) {
        return RpcResultBuilder.<Void> success().build();
    }

    public RpcResult<Void> finish(HashMapDataStoreTransaction transaction) {
        final DataModification<YangInstanceIdentifier, CompositeNode> modification = transaction
                .getModification();
        for (final YangInstanceIdentifier removal : modification
                .getRemovedConfigurationData()) {
            LOG.trace("Removing configuration path {}", removal);
            remove(configuration, removal);
        }
        for (final YangInstanceIdentifier removal : modification
                .getRemovedOperationalData()) {
            LOG.trace("Removing operational path {}", removal);
            remove(operational, removal);
        }
        if (LOG.isTraceEnabled()) {
            for (final YangInstanceIdentifier a : modification
                    .getUpdatedConfigurationData().keySet()) {
                LOG.trace("Adding configuration path {}", a);
            }
            for (final YangInstanceIdentifier a : modification
                    .getUpdatedOperationalData().keySet()) {
                LOG.trace("Adding operational path {}", a);
            }
        }
        configuration.putAll(modification.getUpdatedConfigurationData());
        operational.putAll(modification.getUpdatedOperationalData());

        return RpcResultBuilder.<Void> success().build();
    }

    public void remove(final Map<YangInstanceIdentifier, CompositeNode> map,
            final YangInstanceIdentifier identifier) {
        Set<YangInstanceIdentifier> affected = new HashSet<YangInstanceIdentifier>();
        for (final YangInstanceIdentifier path : map.keySet()) {
            if (identifier.contains(path)) {
                affected.add(path);
            }
        }
        for (final YangInstanceIdentifier pathToRemove : affected) {
            LOG.trace("Removed path {}", pathToRemove);
            map.remove(pathToRemove);
        }
    }

    @Override
    public void close() {
        // NOOP
    }
}
