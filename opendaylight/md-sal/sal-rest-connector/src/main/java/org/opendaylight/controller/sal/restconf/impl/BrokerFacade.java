/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ws.rs.core.Response.Status;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerFacade {
    private final static Logger LOG = LoggerFactory.getLogger(BrokerFacade.class);

    private final static BrokerFacade INSTANCE = new BrokerFacade();
    private volatile ConsumerSession context;
    private DOMDataBroker domDataBroker;

    private BrokerFacade() {
    }

    public void setContext(final ConsumerSession context) {
        this.context = context;
    }

    public static BrokerFacade getInstance() {
        return BrokerFacade.INSTANCE;
    }

    private void checkPreconditions() {
        if (context == null || domDataBroker == null) {
            throw new RestconfDocumentedException(Status.SERVICE_UNAVAILABLE);
        }
    }

    // READ configuration
    public NormalizedNode<?, ?> readConfigurationData(final YangInstanceIdentifier path) {
        checkPreconditions();
        return readDataViaTransaction(domDataBroker.newReadOnlyTransaction(), CONFIGURATION, path);
    }

    public NormalizedNode<?, ?> readConfigurationData(final DOMMountPoint mountPoint, final YangInstanceIdentifier path) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            return readDataViaTransaction(domDataBrokerService.get().newReadOnlyTransaction(), CONFIGURATION, path);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // READ operational
    public NormalizedNode<?, ?> readOperationalData(final YangInstanceIdentifier path) {
        checkPreconditions();
        return readDataViaTransaction(domDataBroker.newReadOnlyTransaction(), OPERATIONAL, path);
    }

    public NormalizedNode<?, ?> readOperationalData(final DOMMountPoint mountPoint, final YangInstanceIdentifier path) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            return readDataViaTransaction(domDataBrokerService.get().newReadOnlyTransaction(), OPERATIONAL, path);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // PUT configuration
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        checkPreconditions();
        DataNormalizationOperation<?> rootOp = ControllerContext.getInstance().getRootOperation();
        return putDataViaTransaction(domDataBroker.newReadWriteTransaction(), CONFIGURATION, path, payload, rootOp);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            DataNormalizationOperation<?> rootOp = new DataNormalizer(mountPoint.getSchemaContext()).getRootOperation();
            return putDataViaTransaction(domDataBrokerService.get().newReadWriteTransaction(), CONFIGURATION, path,
                    payload, rootOp);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // POST configuration
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        checkPreconditions();
        DataNormalizationOperation<?> rootOp = ControllerContext.getInstance().getRootOperation();
        return postDataViaTransaction(domDataBroker.newReadWriteTransaction(), CONFIGURATION, path, payload, rootOp);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            DataNormalizationOperation<?> rootOp = new DataNormalizer(mountPoint.getSchemaContext()).getRootOperation();
            return postDataViaTransaction(domDataBrokerService.get().newReadWriteTransaction(), CONFIGURATION, path,
                    payload, rootOp);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // DELETE configuration
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(
            final YangInstanceIdentifier path) {
        checkPreconditions();
        return deleteDataViaTransaction(domDataBroker.newWriteOnlyTransaction(), CONFIGURATION, path);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            return deleteDataViaTransaction(domDataBrokerService.get().newWriteOnlyTransaction(), CONFIGURATION, path);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // RPC
    public Future<RpcResult<CompositeNode>> invokeRpc(final QName type, final CompositeNode payload) {
        this.checkPreconditions();

        return context.rpc(type, payload);
    }

    public void registerToListenDataChanges(final LogicalDatastoreType datastore, final DataChangeScope scope,
            final ListenerAdapter listener) {
        this.checkPreconditions();

        if (listener.isListening()) {
            return;
        }

        YangInstanceIdentifier path = listener.getPath();
        final ListenerRegistration<DOMDataChangeListener> registration = domDataBroker.registerDataChangeListener(
                datastore, path, listener, scope);

        listener.setRegistration(registration);
    }

    private NormalizedNode<?, ?> readDataViaTransaction(final DOMDataReadTransaction transaction,
            LogicalDatastoreType datastore, YangInstanceIdentifier path) {
        LOG.trace("Read " + datastore.name() + " via Restconf: {}", path);
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> listenableFuture = transaction.read(datastore, path);
        if (listenableFuture != null) {
            Optional<NormalizedNode<?, ?>> optional;
            try {
                LOG.debug("Reading result data from transaction.");
                optional = listenableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RestconfDocumentedException("Problem to get data from transaction.", e.getCause());

            }
            if (optional != null) {
                if (optional.isPresent()) {
                    return optional.get();
                }
            }
        }
        return null;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> postDataViaTransaction(
            final DOMDataReadWriteTransaction rWTransaction, final LogicalDatastoreType datastore,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload, DataNormalizationOperation<?> root) {
        ListenableFuture<Optional<NormalizedNode<?, ?>>> futureDatastoreData = rWTransaction.read(datastore, path);
        try {
            final Optional<NormalizedNode<?, ?>> optionalDatastoreData = futureDatastoreData.get();
            if (optionalDatastoreData.isPresent() && payload.equals(optionalDatastoreData.get())) {
                String errMsg = "Post Configuration via Restconf was not executed because data already exists";
                LOG.trace(errMsg + ":{}", path);
                rWTransaction.cancel();
                throw new RestconfDocumentedException("Data already exists for path: " + path, ErrorType.PROTOCOL,
                        ErrorTag.DATA_EXISTS);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.trace("It wasn't possible to get data loaded from datastore at path " + path);
        }

        ensureParentsByMerge(datastore, path, rWTransaction, root);
        rWTransaction.merge(datastore, path, payload);
        LOG.trace("Post " + datastore.name() + " via Restconf: {}", path);
        return rWTransaction.submit();
    }

    private CheckedFuture<Void, TransactionCommitFailedException> putDataViaTransaction(
            final DOMDataReadWriteTransaction writeTransaction, final LogicalDatastoreType datastore,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload, DataNormalizationOperation<?> root) {
        LOG.trace("Put " + datastore.name() + " via Restconf: {}", path);
        ensureParentsByMerge(datastore, path, writeTransaction, root);
        writeTransaction.put(datastore, path, payload);
        return writeTransaction.submit();
    }

    private CheckedFuture<Void, TransactionCommitFailedException> deleteDataViaTransaction(
            final DOMDataWriteTransaction writeTransaction, final LogicalDatastoreType datastore,
            YangInstanceIdentifier path) {
        LOG.trace("Delete " + datastore.name() + " via Restconf: {}", path);
        writeTransaction.delete(datastore, path);
        return writeTransaction.submit();
    }

    public void setDomDataBroker(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    private final void ensureParentsByMerge(final LogicalDatastoreType store,
            final YangInstanceIdentifier normalizedPath, final DOMDataReadWriteTransaction rwTx,
            final DataNormalizationOperation<?> root) {
        List<PathArgument> currentArguments = new ArrayList<>();
        Iterator<PathArgument> iterator = normalizedPath.getPathArguments().iterator();
        DataNormalizationOperation<?> currentOp = root;
        while (iterator.hasNext()) {
            PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (DataNormalizationException e) {
                rwTx.cancel();
                throw new IllegalArgumentException(
                        String.format("Invalid child encountered in path %s", normalizedPath), e);
            }
            currentArguments.add(currentArg);
            YangInstanceIdentifier currentPath = YangInstanceIdentifier.create(currentArguments);

            final Boolean exists;

            try {

                CheckedFuture<Boolean, ReadFailedException> future = rwTx.exists(store, currentPath);
                exists = future.checkedGet();
            } catch (ReadFailedException e) {
                LOG.error("Failed to read pre-existing data from store {} path {}", store, currentPath, e);
                rwTx.cancel();
                throw new IllegalStateException("Failed to read pre-existing data", e);
            }

            if (!exists && iterator.hasNext()) {
                rwTx.merge(store, currentPath, currentOp.createDefault(currentArg));
            }
        }
    }
}
