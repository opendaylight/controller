/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.impl.AbstractDataModification;
import org.opendaylight.controller.md.sal.common.impl.util.AbstractLockableDelegator;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.dom.broker.util.YangDataOperations;
import org.opendaylight.controller.sal.dom.broker.util.YangSchemaUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

public class SchemaAwareDataStoreAdapter extends AbstractLockableDelegator<DataStore> implements //
        DataStore, //
        SchemaServiceListener, //
        SchemaContextListener, //
        AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(SchemaAwareDataStoreAdapter.class);

    private SchemaContext schema = null;
    private boolean validationEnabled = false;
    private final DataReader<InstanceIdentifier, CompositeNode> reader = new MergeFirstLevelReader();

    @Override
    public boolean containsConfigurationPath(InstanceIdentifier path) {
        try {
            getDelegateReadLock().lock();
            return getDelegate().containsConfigurationPath(path);

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public boolean containsOperationalPath(InstanceIdentifier path) {
        try {
            getDelegateReadLock().lock();
            return getDelegate().containsOperationalPath(path);

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public Iterable<InstanceIdentifier> getStoredConfigurationPaths() {
        try {
            getDelegateReadLock().lock();
            return getDelegate().getStoredConfigurationPaths();

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public Iterable<InstanceIdentifier> getStoredOperationalPaths() {
        try {
            getDelegateReadLock().lock();
            return getDelegate().getStoredOperationalPaths();

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public CompositeNode readConfigurationData(InstanceIdentifier path) {
        return reader.readConfigurationData(path);
    }

    @Override
    public CompositeNode readOperationalData(InstanceIdentifier path) {
        return reader.readOperationalData(path);
    }

    @Override
    public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier, CompositeNode> requestCommit(
            DataModification<InstanceIdentifier, CompositeNode> modification) {
        validateAgainstSchema(modification);
        NormalizedDataModification cleanedUp = prepareMergedTransaction(modification);
        cleanedUp.status = TransactionStatus.SUBMITED;
        return retrieveDelegate().requestCommit(cleanedUp);
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    private void validateAgainstSchema(DataModification<InstanceIdentifier, CompositeNode> modification) {
        if (!validationEnabled) {
            return;
        }

        if (schema == null) {
            LOG.warn("Validation not performed for {}. Reason: YANG Schema not present.", modification.getIdentifier());
            return;
        }
    }

    @Override
    protected void onDelegateChanged(DataStore oldDelegate, DataStore newDelegate) {
        // NOOP
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        this.schema = context;
    }

    @Override
    public void close() throws Exception {
        this.schema = null;
    }

    protected CompositeNode mergeData(InstanceIdentifier path, CompositeNode stored, CompositeNode modified,
            boolean config) {
        // long startTime = System.nanoTime();
        try {
            DataSchemaNode node = schemaNodeFor(path);
            return YangDataOperations.merge(node, stored, modified, config);
        } finally {
            // System.out.println("Merge time: " + ((System.nanoTime() -
            // startTime) / 1000.0d));
        }
    }

    private DataSchemaNode schemaNodeFor(InstanceIdentifier path) {
        checkState(schema != null, "YANG Schema is not available");
        return YangSchemaUtils.getSchemaNode(schema, path);
    }

    private NormalizedDataModification prepareMergedTransaction(
            DataModification<InstanceIdentifier, CompositeNode> original) {
        NormalizedDataModification normalized = new NormalizedDataModification(original);
        for (Entry<InstanceIdentifier, CompositeNode> entry : original.getUpdatedConfigurationData().entrySet()) {
            normalized.putConfigurationData(entry.getKey(), entry.getValue());
        }
        for (Entry<InstanceIdentifier, CompositeNode> entry : original.getUpdatedOperationalData().entrySet()) {
            normalized.putOperationalData(entry.getKey(), entry.getValue());
        }
        for (InstanceIdentifier entry : original.getRemovedConfigurationData()) {
            normalized.deepRemoveConfigurationData(entry);
        }
        for (InstanceIdentifier entry : original.getRemovedOperationalData()) {
            normalized.deepRemoveOperationalData(entry);
        }
        return normalized;
    }

    private Iterable<InstanceIdentifier> getConfigurationSubpaths(InstanceIdentifier entry) {
        // FIXME: This should be replaced by index
        Iterable<InstanceIdentifier> paths = getStoredConfigurationPaths();

        return getChildrenPaths(entry, paths);

    }

    public Iterable<InstanceIdentifier> getOperationalSubpaths(InstanceIdentifier entry) {
        // FIXME: This should be indexed
        Iterable<InstanceIdentifier> paths = getStoredOperationalPaths();

        return getChildrenPaths(entry, paths);
    }

    private static final Iterable<InstanceIdentifier> getChildrenPaths(InstanceIdentifier entry,
            Iterable<InstanceIdentifier> paths) {
        ImmutableSet.Builder<InstanceIdentifier> children = ImmutableSet.builder();
        for (InstanceIdentifier potential : paths) {
            if (entry.contains(potential)) {
                children.add(entry);
            }
        }
        return children.build();
    }

    private final Comparator<Entry<InstanceIdentifier, CompositeNode>> preparationComparator = new Comparator<Entry<InstanceIdentifier, CompositeNode>>() {
        @Override
        public int compare(Entry<InstanceIdentifier, CompositeNode> o1, Entry<InstanceIdentifier, CompositeNode> o2) {
            InstanceIdentifier o1Key = o1.getKey();
            InstanceIdentifier o2Key = o2.getKey();
            return Integer.compare(o1Key.getPath().size(), o2Key.getPath().size());
        }
    };

    private class MergeFirstLevelReader implements DataReader<InstanceIdentifier, CompositeNode> {

        @Override
        public CompositeNode readConfigurationData(final InstanceIdentifier path) {
            getDelegateReadLock().lock();
            try {
                if (path.getPath().isEmpty()) {
                    return null;
                }
                QName qname = null;
                CompositeNode original = getDelegate().readConfigurationData(path);
                ArrayList<Node<?>> childNodes = new ArrayList<Node<?>>();
                if (original != null) {
                    childNodes.addAll(original.getChildren());
                    qname = original.getNodeType();
                } else {
                    qname = path.getPath().get(path.getPath().size() - 1).getNodeType();
                }

                FluentIterable<InstanceIdentifier> directChildren = FluentIterable.from(getStoredConfigurationPaths())
                        .filter(new Predicate<InstanceIdentifier>() {
                            @Override
                            public boolean apply(InstanceIdentifier input) {
                                if (path.contains(input)) {
                                    int nesting = input.getPath().size() - path.getPath().size();
                                    if (nesting == 1) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
                for (InstanceIdentifier instanceIdentifier : directChildren) {
                    childNodes.add(getDelegate().readConfigurationData(instanceIdentifier));
                }
                if (original == null && childNodes.isEmpty()) {
                    return null;
                }

                return new CompositeNodeTOImpl(qname, null, childNodes);
            } finally {
                getDelegateReadLock().unlock();
            }
        }

        @Override
        public CompositeNode readOperationalData(final InstanceIdentifier path) {
            getDelegateReadLock().lock();
            try {
                if (path.getPath().isEmpty()) {
                    return null;
                }
                QName qname = null;
                CompositeNode original = getDelegate().readOperationalData(path);
                ArrayList<Node<?>> childNodes = new ArrayList<Node<?>>();
                if (original != null) {
                    childNodes.addAll(original.getChildren());
                    qname = original.getNodeType();
                } else {
                    qname = path.getPath().get(path.getPath().size() - 1).getNodeType();
                }

                FluentIterable<InstanceIdentifier> directChildren = FluentIterable.from(getStoredOperationalPaths())
                        .filter(new Predicate<InstanceIdentifier>() {
                            @Override
                            public boolean apply(InstanceIdentifier input) {
                                if (path.contains(input)) {
                                    int nesting = input.getPath().size() - path.getPath().size();
                                    if (nesting == 1) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });

                for (InstanceIdentifier instanceIdentifier : directChildren) {
                    childNodes.add(getDelegate().readOperationalData(instanceIdentifier));
                }
                if (original == null && childNodes.isEmpty()) {
                    return null;
                }

                return new CompositeNodeTOImpl(qname, null, childNodes);
            } finally {
                getDelegateReadLock().unlock();
            }
        }
    }

    private class NormalizedDataModification extends AbstractDataModification<InstanceIdentifier, CompositeNode> {

        private final Object identifier;
        private TransactionStatus status;

        public NormalizedDataModification(DataModification<InstanceIdentifier, CompositeNode> original) {
            super(getDelegate());
            identifier = original;
            status = TransactionStatus.NEW;
        }

        /**
         *
         * Ensures all subpaths are removed - this currently does slow lookup in
         * all keys.
         *
         * @param entry
         */
        public void deepRemoveOperationalData(InstanceIdentifier entry) {
            Iterable<InstanceIdentifier> paths = getOperationalSubpaths(entry);
            removeOperationalData(entry);
            for (InstanceIdentifier potential : paths) {
                removeOperationalData(potential);
            }
        }

        public void deepRemoveConfigurationData(InstanceIdentifier entry) {
            Iterable<InstanceIdentifier> paths = getConfigurationSubpaths(entry);
            removeConfigurationData(entry);
            for (InstanceIdentifier potential : paths) {
                removeConfigurationData(potential);
            }
        }

        @Override
        public Object getIdentifier() {
            return this.identifier;
        }

        @Override
        public TransactionStatus getStatus() {
            return status;
        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            throw new UnsupportedOperationException("Commit should not be invoked on this");
        }

        @Override
        protected CompositeNode mergeConfigurationData(InstanceIdentifier path, CompositeNode stored,
                CompositeNode modified) {
            return mergeData(path, stored, modified, true);
        }

        @Override
        protected CompositeNode mergeOperationalData(InstanceIdentifier path, CompositeNode stored,
                CompositeNode modified) {
            return mergeData(path, stored, modified, false);
        }
    }

}
