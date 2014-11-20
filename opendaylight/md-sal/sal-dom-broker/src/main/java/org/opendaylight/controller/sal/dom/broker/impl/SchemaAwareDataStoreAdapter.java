/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class SchemaAwareDataStoreAdapter extends AbstractLockableDelegator<DataStore> implements DataStore, SchemaContextListener, AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(SchemaAwareDataStoreAdapter.class);

    private SchemaContext schema = null;
    private boolean validationEnabled = false;
    private final DataReader<YangInstanceIdentifier, CompositeNode> reader = new MergeFirstLevelReader();

    @Override
    public boolean containsConfigurationPath(final YangInstanceIdentifier path) {
        try {
            getDelegateReadLock().lock();
            return getDelegate().containsConfigurationPath(path);

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public boolean containsOperationalPath(final YangInstanceIdentifier path) {
        try {
            getDelegateReadLock().lock();
            return getDelegate().containsOperationalPath(path);

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public Iterable<YangInstanceIdentifier> getStoredConfigurationPaths() {
        try {
            getDelegateReadLock().lock();
            return getDelegate().getStoredConfigurationPaths();

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public Iterable<YangInstanceIdentifier> getStoredOperationalPaths() {
        try {
            getDelegateReadLock().lock();
            return getDelegate().getStoredOperationalPaths();

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public CompositeNode readConfigurationData(final YangInstanceIdentifier path) {
        return reader.readConfigurationData(path);
    }

    @Override
    public CompositeNode readOperationalData(final YangInstanceIdentifier path) {
        return reader.readOperationalData(path);
    }

    @Override
    public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<YangInstanceIdentifier, CompositeNode> requestCommit(
            final DataModification<YangInstanceIdentifier, CompositeNode> modification) {
        validateAgainstSchema(modification);
        NormalizedDataModification cleanedUp = prepareMergedTransaction(modification);
        cleanedUp.status = TransactionStatus.SUBMITED;
        return retrieveDelegate().requestCommit(cleanedUp);
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(final boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    private void validateAgainstSchema(final DataModification<YangInstanceIdentifier, CompositeNode> modification) {
        if (!validationEnabled) {
            return;
        }

        if (schema == null) {
            LOG.warn("Validation not performed for {}. Reason: YANG Schema not present.", modification.getIdentifier());
            return;
        }
    }

    @Override
    protected void onDelegateChanged(final DataStore oldDelegate, final DataStore newDelegate) {
        // NOOP
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext context) {
        this.schema = context;
    }

    @Override
    public void close() throws Exception {
        this.schema = null;
    }

    protected CompositeNode mergeData(final YangInstanceIdentifier path, final CompositeNode stored, final CompositeNode modified,
            final boolean config) {
        // long startTime = System.nanoTime();
        try {
            DataSchemaNode node = schemaNodeFor(path);
            return YangDataOperations.merge(node, stored, modified, config);
        } finally {
            // System.out.println("Merge time: " + ((System.nanoTime() -
            // startTime) / 1000.0d));
        }
    }

    private DataSchemaNode schemaNodeFor(final YangInstanceIdentifier path) {
        checkState(schema != null, "YANG Schema is not available");
        return YangSchemaUtils.getSchemaNode(schema, path);
    }

    private NormalizedDataModification prepareMergedTransaction(
            final DataModification<YangInstanceIdentifier, CompositeNode> original) {
        NormalizedDataModification normalized = new NormalizedDataModification(original);
        LOG.trace("Transaction: {} Removed Configuration {}, Removed Operational {}", original.getIdentifier(),
                original.getRemovedConfigurationData(), original.getRemovedConfigurationData());
        LOG.trace("Transaction: {} Created Configuration {}, Created Operational {}", original.getIdentifier(),
                original.getCreatedConfigurationData().entrySet(), original.getCreatedOperationalData().entrySet());
        LOG.trace("Transaction: {} Updated Configuration {}, Updated Operational {}", original.getIdentifier(),
                original.getUpdatedConfigurationData().entrySet(), original.getUpdatedOperationalData().entrySet());

        for (YangInstanceIdentifier entry : original.getRemovedConfigurationData()) {
            normalized.deepRemoveConfigurationData(entry);
        }
        for (YangInstanceIdentifier entry : original.getRemovedOperationalData()) {
            normalized.deepRemoveOperationalData(entry);
        }
        for (Entry<YangInstanceIdentifier, CompositeNode> entry : original.getUpdatedConfigurationData().entrySet()) {
            normalized.putDeepConfigurationData(entry.getKey(), entry.getValue());
        }
        for (Entry<YangInstanceIdentifier, CompositeNode> entry : original.getUpdatedOperationalData().entrySet()) {
            normalized.putDeepOperationalData(entry.getKey(), entry.getValue());
        }
        return normalized;
    }

    private Iterable<YangInstanceIdentifier> getConfigurationSubpaths(final YangInstanceIdentifier entry) {
        // FIXME: This should be replaced by index
        Iterable<YangInstanceIdentifier> paths = getStoredConfigurationPaths();

        return getChildrenPaths(entry, paths);

    }

    public Iterable<YangInstanceIdentifier> getOperationalSubpaths(final YangInstanceIdentifier entry) {
        // FIXME: This should be indexed
        Iterable<YangInstanceIdentifier> paths = getStoredOperationalPaths();

        return getChildrenPaths(entry, paths);
    }

    private static final Iterable<YangInstanceIdentifier> getChildrenPaths(final YangInstanceIdentifier entry,
            final Iterable<YangInstanceIdentifier> paths) {
        ImmutableSet.Builder<YangInstanceIdentifier> children = ImmutableSet.builder();
        for (YangInstanceIdentifier potential : paths) {
            if (entry.contains(potential)) {
                children.add(entry);
            }
        }
        return children.build();
    }

    private final Comparator<Entry<YangInstanceIdentifier, CompositeNode>> preparationComparator = new Comparator<Entry<YangInstanceIdentifier, CompositeNode>>() {
        @Override
        public int compare(final Entry<YangInstanceIdentifier, CompositeNode> o1, final Entry<YangInstanceIdentifier, CompositeNode> o2) {
            YangInstanceIdentifier o1Key = o1.getKey();
            YangInstanceIdentifier o2Key = o2.getKey();
            return Integer.compare(Iterables.size(o1Key.getPathArguments()), Iterables.size(o2Key.getPathArguments()));
        }
    };

    private class MergeFirstLevelReader implements DataReader<YangInstanceIdentifier, CompositeNode> {

        @Override
        public CompositeNode readConfigurationData(final YangInstanceIdentifier path) {
            getDelegateReadLock().lock();
            try {
                if (Iterables.isEmpty(path.getPathArguments())) {
                    return null;
                }
                QName qname = null;
                CompositeNode original = getDelegate().readConfigurationData(path);
                ArrayList<Node<?>> childNodes = new ArrayList<Node<?>>();
                if (original != null) {
                    childNodes.addAll(original.getValue());
                    qname = original.getNodeType();
                } else {
                    qname = path.getLastPathArgument().getNodeType();
                }

                FluentIterable<YangInstanceIdentifier> directChildren = FluentIterable.from(getStoredConfigurationPaths())
                        .filter(new Predicate<YangInstanceIdentifier>() {
                            @Override
                            public boolean apply(final YangInstanceIdentifier input) {
                                if (path.contains(input)) {
                                    int nesting = Iterables.size(input.getPathArguments()) - Iterables.size(path.getPathArguments());
                                    if (nesting == 1) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
                for (YangInstanceIdentifier instanceIdentifier : directChildren) {
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
        public CompositeNode readOperationalData(final YangInstanceIdentifier path) {
            getDelegateReadLock().lock();
            try {
                if (Iterables.isEmpty(path.getPathArguments())) {
                    return null;
                }
                QName qname = null;
                CompositeNode original = getDelegate().readOperationalData(path);
                ArrayList<Node<?>> childNodes = new ArrayList<Node<?>>();
                if (original != null) {
                    childNodes.addAll(original.getValue());
                    qname = original.getNodeType();
                } else {
                    qname = path.getLastPathArgument().getNodeType();
                }

                FluentIterable<YangInstanceIdentifier> directChildren = FluentIterable.from(getStoredOperationalPaths())
                        .filter(new Predicate<YangInstanceIdentifier>() {
                            @Override
                            public boolean apply(final YangInstanceIdentifier input) {
                                if (path.contains(input)) {
                                    int nesting = Iterables.size(input.getPathArguments()) - Iterables.size(path.getPathArguments());
                                    if (nesting == 1) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });

                for (YangInstanceIdentifier instanceIdentifier : directChildren) {
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

    private class NormalizedDataModification extends AbstractDataModification<YangInstanceIdentifier, CompositeNode> {

        private final String CONFIGURATIONAL_DATA_STORE_MARKER = "configurational";
        private final String OPERATIONAL_DATA_STORE_MARKER = "operational";
        private final Object identifier;
        private TransactionStatus status;

        public NormalizedDataModification(final DataModification<YangInstanceIdentifier, CompositeNode> original) {
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
        public void deepRemoveOperationalData(final YangInstanceIdentifier entry) {
            Iterable<YangInstanceIdentifier> paths = getOperationalSubpaths(entry);
            removeOperationalData(entry);
            for (YangInstanceIdentifier potential : paths) {
                removeOperationalData(potential);
            }
        }

        public void deepRemoveConfigurationData(final YangInstanceIdentifier entry) {
            Iterable<YangInstanceIdentifier> paths = getConfigurationSubpaths(entry);
            removeConfigurationData(entry);
            for (YangInstanceIdentifier potential : paths) {
                removeConfigurationData(potential);
            }
        }

        public void putDeepConfigurationData(final YangInstanceIdentifier entryKey, final CompositeNode entryData) {
            this.putCompositeNodeData(entryKey, entryData, CONFIGURATIONAL_DATA_STORE_MARKER);
        }

        public void putDeepOperationalData(final YangInstanceIdentifier entryKey, final CompositeNode entryData) {
            this.putCompositeNodeData(entryKey, entryData, OPERATIONAL_DATA_STORE_MARKER);
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
        protected CompositeNode mergeConfigurationData(final YangInstanceIdentifier path, final CompositeNode stored,
                final CompositeNode modified) {
            return mergeData(path, stored, modified, true);
        }

        @Override
        protected CompositeNode mergeOperationalData(final YangInstanceIdentifier path, final CompositeNode stored,
                final CompositeNode modified) {
            return mergeData(path, stored, modified, false);
        }

        private void putData(final YangInstanceIdentifier entryKey, final CompositeNode entryData, final String dataStoreIdentifier) {
            if (dataStoreIdentifier != null && entryKey != null && entryData != null) {
                switch (dataStoreIdentifier) {
                case (CONFIGURATIONAL_DATA_STORE_MARKER):
                    this.putConfigurationData(entryKey, entryData);
                break;
                case (OPERATIONAL_DATA_STORE_MARKER):
                    this.putOperationalData(entryKey, entryData);
                break;

                default:
                    LOG.error(dataStoreIdentifier + " is NOT valid DataStore switch marker");
                    throw new RuntimeException(dataStoreIdentifier + " is NOT valid DataStore switch marker");
                }
            }
        }

        private void putCompositeNodeData(final YangInstanceIdentifier entryKey, final CompositeNode entryData,
                final String dataStoreIdentifier) {
            this.putData(entryKey, entryData, dataStoreIdentifier);

            for (Node<?> child : entryData.getValue()) {
                YangInstanceIdentifier subEntryId = YangInstanceIdentifier.builder(entryKey).node(child.getNodeType())
                        .toInstance();
                if (child instanceof CompositeNode) {
                    DataSchemaNode subSchema = schemaNodeFor(subEntryId);
                    CompositeNode compNode = (CompositeNode) child;
                    YangInstanceIdentifier instanceId = null;

                    if (subSchema instanceof ListSchemaNode) {
                        ListSchemaNode listSubSchema = (ListSchemaNode) subSchema;
                        Map<QName, Object> mapOfSubValues = this.getValuesFromListSchema(listSubSchema,
                                (CompositeNode) child);
                        if (mapOfSubValues != null) {
                            instanceId = YangInstanceIdentifier.builder(entryKey)
                                    .nodeWithKey(listSubSchema.getQName(), mapOfSubValues).toInstance();
                        }
                    } else if (subSchema instanceof ContainerSchemaNode) {
                        ContainerSchemaNode containerSchema = (ContainerSchemaNode) subSchema;
                        instanceId = YangInstanceIdentifier.builder(entryKey).node(subSchema.getQName()).toInstance();
                    }
                    if (instanceId != null) {
                        this.putCompositeNodeData(instanceId, compNode, dataStoreIdentifier);
                    }
                }
            }
        }

        private Map<QName, Object> getValuesFromListSchema(final ListSchemaNode listSchema, final CompositeNode entryData) {
            List<QName> keyDef = listSchema.getKeyDefinition();
            if (keyDef != null && !keyDef.isEmpty()) {
                Map<QName, Object> map = new HashMap<QName, Object>();
                for (QName key : keyDef) {
                    List<Node<?>> data = entryData.get(key);
                    if (data != null && !data.isEmpty()) {
                        for (Node<?> nodeData : data) {
                            if (nodeData instanceof SimpleNode<?>) {
                                map.put(key, data.get(0).getValue());
                            }
                        }
                    }
                }
                return map;
            }
            return null;
        }
    }
}
