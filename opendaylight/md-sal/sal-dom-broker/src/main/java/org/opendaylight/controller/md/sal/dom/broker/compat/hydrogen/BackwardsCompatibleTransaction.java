/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public abstract class BackwardsCompatibleTransaction<T extends DOMDataReadTransaction> implements DataModificationTransaction, Delegator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(BackwardsCompatibleTransaction.class);

    private final T asyncTx;
    private final DataNormalizer normalizer;

    protected BackwardsCompatibleTransaction(final T asyncTx, final DataNormalizer normalizer) {
        super();
        this.asyncTx = asyncTx;
        this.normalizer = normalizer;
    }

    public static BackwardsCompatibleTransaction<?> readOnlyTransaction(final DOMDataReadOnlyTransaction readTx,
            final DataNormalizer normalizer) {

        return new BackwardsCompatibleTransaction<DOMDataReadOnlyTransaction>(readTx, normalizer) {

            @Override
            public TransactionStatus getStatus() {
                return TransactionStatus.NEW;
            }

            @Override
            public Future<RpcResult<TransactionStatus>> commit() {
                getDelegate().close();
                return null;
            }
        };
    }

    public static BackwardsCompatibleTransaction<?> readWriteTransaction(final DOMDataReadWriteTransaction rwTx,
            final DataNormalizer normalizer) {
        return new ReadWriteTransaction(rwTx, normalizer);
    }

    protected DataNormalizer getNormalizer() {
        return normalizer;
    }

    @Override
    public T getDelegate() {
        return asyncTx;
    };

    @Override
    public CompositeNode readConfigurationData(final YangInstanceIdentifier legacyPath) {

        YangInstanceIdentifier normalizedPath = normalizer.toNormalized(legacyPath);

        ListenableFuture<Optional<NormalizedNode<?, ?>>> normalizedData = asyncTx.read(
                LogicalDatastoreType.CONFIGURATION, normalizedPath);

        try {
            return normalizer.toLegacy(normalizedPath, normalizedData.get().orNull());
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public CompositeNode readOperationalData(final YangInstanceIdentifier legacyPath) {
        YangInstanceIdentifier normalizedPath = normalizer.toNormalized(legacyPath);

        ListenableFuture<Optional<NormalizedNode<?, ?>>> normalizedData = asyncTx.read(
                LogicalDatastoreType.OPERATIONAL, normalizedPath);

        try {
            return normalizer.toLegacy(normalizedPath, normalizedData.get().orNull());
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public ListenerRegistration<DataTransactionListener> registerListener(final DataTransactionListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<YangInstanceIdentifier, CompositeNode> getCreatedConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<YangInstanceIdentifier, CompositeNode> getCreatedOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<YangInstanceIdentifier, CompositeNode> getOriginalConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<YangInstanceIdentifier, CompositeNode> getOriginalOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public Set<YangInstanceIdentifier> getRemovedConfigurationData() {
        return Collections.emptySet();
    }

    @Override
    public Set<YangInstanceIdentifier> getRemovedOperationalData() {
        return Collections.emptySet();
    }

    @Override
    public Map<YangInstanceIdentifier, CompositeNode> getUpdatedConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<YangInstanceIdentifier, CompositeNode> getUpdatedOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public void putConfigurationData(final YangInstanceIdentifier path, final CompositeNode data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putOperationalData(final YangInstanceIdentifier path, final CompositeNode data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeConfigurationData(final YangInstanceIdentifier path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeOperationalData(final YangInstanceIdentifier path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getIdentifier() {
        return asyncTx.getIdentifier();
    }

    private static final class ReadWriteTransaction extends BackwardsCompatibleTransaction<DOMDataReadWriteTransaction> {

        private TransactionStatus status = TransactionStatus.NEW;

        protected ReadWriteTransaction(final DOMDataReadWriteTransaction asyncTx, final DataNormalizer normalizer) {
            super(asyncTx, normalizer);
        }

        @Override
        public TransactionStatus getStatus() {
            return status;
        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            Preconditions.checkState(status == TransactionStatus.NEW);
            status = TransactionStatus.SUBMITED;
            return AbstractDataTransaction.convertToLegacyCommitFuture(getDelegate().submit());
        }

        @Override
        public void putConfigurationData(final YangInstanceIdentifier legacyPath, final CompositeNode legacyData) {
            checkNotNull(legacyPath, "Path MUST NOT be null.");
            checkNotNull(legacyData, "Data for path %s MUST NOT be null",legacyData);
            Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedData = getNormalizer().toNormalized(legacyPath, legacyData);
            putWithEnsuredParents(LogicalDatastoreType.CONFIGURATION, normalizedData.getKey(), normalizedData.getValue());
        }

        @Override
        public void putOperationalData(final YangInstanceIdentifier legacyPath, final CompositeNode legacyData) {
            checkNotNull(legacyPath, "Path MUST NOT be null.");
            checkNotNull(legacyData, "Data for path %s MUST NOT be null",legacyData);
            Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedData = getNormalizer().toNormalized(legacyPath, legacyData);
            putWithEnsuredParents(LogicalDatastoreType.OPERATIONAL, normalizedData.getKey(), normalizedData.getValue());
        }

        private void putWithEnsuredParents(final LogicalDatastoreType store, final YangInstanceIdentifier normalizedPath,
                final NormalizedNode<?, ?> normalizedData) {

            LOG.trace("write {}:{} ",store,normalizedPath);
            try {
                List<PathArgument> currentArguments = new ArrayList<>();
                DataNormalizationOperation<?> currentOp = getNormalizer().getRootOperation();
                Iterator<PathArgument> iterator = normalizedPath.getPathArguments().iterator();
                while(iterator.hasNext()) {
                    PathArgument currentArg = iterator.next();
                    try {
                        currentOp = currentOp.getChild(currentArg);
                    } catch (DataNormalizationException e) {
                        throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", normalizedPath), e);
                    }
                    currentArguments.add(currentArg);
                    YangInstanceIdentifier currentPath = YangInstanceIdentifier.create(currentArguments);
                    boolean isPresent = getDelegate().read(store, currentPath).get().isPresent();
                    if(isPresent == false && iterator.hasNext()) {
                        getDelegate().merge(store, currentPath, currentOp.createDefault(currentArg));
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Exception durring read.",e);
            }

            getDelegate().put(store, normalizedPath, normalizedData);
        }

        @Override
        public void removeConfigurationData(final YangInstanceIdentifier legacyPath) {
            checkNotNull(legacyPath, "Path MUST NOT be null.");
            getDelegate().delete(LogicalDatastoreType.CONFIGURATION, getNormalizer().toNormalized(legacyPath));
        }

        @Override
        public void removeOperationalData(final YangInstanceIdentifier legacyPath) {
            checkNotNull(legacyPath, "Path MUST NOT be null.");
            getDelegate().delete(LogicalDatastoreType.OPERATIONAL, getNormalizer().toNormalized(legacyPath));
        }
    }
}
