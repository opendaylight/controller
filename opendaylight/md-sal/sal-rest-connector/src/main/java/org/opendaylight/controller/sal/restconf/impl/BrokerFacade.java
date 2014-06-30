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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ws.rs.core.Response.Status;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerFacade implements DataReader<InstanceIdentifier, NormalizedNode<?, ?>> {
    private final static Logger LOG = LoggerFactory.getLogger(BrokerFacade.class);

    private final static BrokerFacade INSTANCE = new BrokerFacade();

    private volatile DataBrokerService dataService;
    private volatile ConsumerSession context;
    private DOMDataBroker domDataBroker;

    private BrokerFacade() {
    }

    public void setContext(final ConsumerSession context) {
        this.context = context;
    }

    public void setDataService(final DataBrokerService dataService) {
        this.dataService = dataService;
    }

    public static BrokerFacade getInstance() {
        return BrokerFacade.INSTANCE;
    }

    private void checkPreconditions() {
        if (context == null || dataService == null || domDataBroker == null) {
            throw new RestconfDocumentedException(Status.SERVICE_UNAVAILABLE);
        }
    }

    // READ configuration
    @Override
    public NormalizedNode<?, ?> readConfigurationData(final InstanceIdentifier path) {
        checkPreconditions();
        return readDataViaTransaction(domDataBroker.newReadOnlyTransaction(), CONFIGURATION, path);
    }

    public NormalizedNode<?, ?> readConfigurationData(final MountInstance mountPoint, final InstanceIdentifier path) {
        throw new UnsupportedOperationException("readConfigurationDataBehindMountPoint() implementation missing.");
        // this.checkPreconditions();
        //
        // LOG.trace( "Read Configuration via Restconf: {}", path );
        //
        // return mountPoint.readConfigurationData( path );
    }

    // READ operational
    @Override
    public NormalizedNode<?, ?> readOperationalData(final InstanceIdentifier path) {
        checkPreconditions();
        return readDataViaTransaction(domDataBroker.newReadOnlyTransaction(), OPERATIONAL, path);
    }

    public NormalizedNode<?, ?> readOperationalData(final MountInstance mountPoint, final InstanceIdentifier path) {
        throw new UnsupportedOperationException("readOperationalDataBehindMountPoint() implementation missing.");
        // this.checkPreconditions();
        //
        // BrokerFacade.LOG.trace("Read Operational via Restconf: {}", path);
        //
        // return mountPoint.readOperationalData(path);
    }

    // PUT configuration
    public Future<RpcResult<TransactionStatus>> commitConfigurationDataPut(final InstanceIdentifier path,
            final NormalizedNode<?, ?> payload) {
        checkPreconditions();
        return putDataViaTransaction(domDataBroker.newWriteOnlyTransaction(), CONFIGURATION, path, payload);
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataPut(final MountInstance mountPoint,
            final InstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        throw new UnsupportedOperationException("commitConfigurationDataPut() with mount point isn't implemented.");
        // this.checkPreconditions();
        //
        // final DataModificationTransaction transaction =
        // mountPoint.beginTransaction();
        // BrokerFacade.LOG.trace("Put Configuration via Restconf: {}", path);
        // transaction.putConfigurationData(path, payload);
        // return transaction.commit();
    }

    // POST configuration
    public Future<RpcResult<TransactionStatus>> commitConfigurationDataPost(final InstanceIdentifier path,
            final NormalizedNode<?, ?> payload) {
        checkPreconditions();
        return postDataViaTransaction(domDataBroker.newReadWriteTransaction(), CONFIGURATION, path, payload);
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataPost(final MountInstance mountPoint,
            final InstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        throw new UnsupportedOperationException("commitConfigurationDataPost() with mount point isn't implemented");
        // this.checkPreconditions();
        //
        // final DataModificationTransaction transaction =
        // mountPoint.beginTransaction();
        // /* check for available Node in Configuration DataStore by path */
        // CompositeNode availableNode =
        // transaction.readConfigurationData(path);
        // if (availableNode != null) {
        // String errMsg =
        // "Post Configuration via Restconf was not executed because data already exists";
        // BrokerFacade.LOG.warn((new
        // StringBuilder(errMsg)).append(" : ").append(path).toString());
        //
        // throw new
        // RestconfDocumentedException("Data already exists for path: " + path,
        // ErrorType.PROTOCOL,
        // ErrorTag.DATA_EXISTS);
        // }
        // BrokerFacade.LOG.trace("Post Configuration via Restconf: {}", path);
        // transaction.putConfigurationData(path, payload);
        // return transaction.commit();
    }

    // DELETE configuration
    public Future<RpcResult<TransactionStatus>> commitConfigurationDataDelete(final InstanceIdentifier path) {
        checkPreconditions();
        return deleteDataViaTransaction(domDataBroker.newWriteOnlyTransaction(), CONFIGURATION, path);
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataDelete(final MountInstance mountPoint,
            final InstanceIdentifier path) {
        throw new UnsupportedOperationException("commitConfigurationDataDelete() method isn't implemented");
        // this.checkPreconditions();
        //
        // final DataModificationTransaction transaction =
        // mountPoint.beginTransaction();
        // LOG.info("Delete Configuration via Restconf: {}", path);
        // transaction.removeConfigurationData(path);
        // return transaction.commit();
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

        InstanceIdentifier path = listener.getPath();
        final ListenerRegistration<DOMDataChangeListener> registration = domDataBroker.registerDataChangeListener(
                datastore, path, listener, scope);

        listener.setRegistration(registration);
    }

    private NormalizedNode<?, ?> readDataViaTransaction(final DOMDataReadTransaction transaction,
            LogicalDatastoreType datastore, InstanceIdentifier path) {
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

    private Future<RpcResult<TransactionStatus>> postDataViaTransaction(
            final DOMDataReadWriteTransaction rWTransaction, final LogicalDatastoreType datastore,
            final InstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        ListenableFuture<Optional<NormalizedNode<?, ?>>> futureDatastoreData = rWTransaction.read(datastore, path);
        try {
            final Optional<NormalizedNode<?, ?>> optionalDatastoreData = futureDatastoreData.get();
            if (optionalDatastoreData.isPresent() && payload.equals(optionalDatastoreData.get())) {
                String errMsg = "Post Configuration via Restconf was not executed because data already exists";
                LOG.trace(errMsg + ":{}", path);
                throw new RestconfDocumentedException("Data already exists for path: " + path, ErrorType.PROTOCOL,
                        ErrorTag.DATA_EXISTS);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.trace("It wasn't possible to get data loaded from datastore at path " + path);
        }
        rWTransaction.merge(datastore, path, payload);
        LOG.trace("Post " + datastore.name() + " via Restconf: {}", path);
        return rWTransaction.commit();
    }

    private Future<RpcResult<TransactionStatus>> putDataViaTransaction(final DOMDataWriteTransaction writeTransaction,
            final LogicalDatastoreType datastore, final InstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        LOG.trace("Put " + datastore.name() + " via Restconf: {}", path);
        writeTransaction.put(datastore, path, payload);
        return writeTransaction.commit();
    }

    private ListenableFuture<RpcResult<TransactionStatus>> deleteDataViaTransaction(
            final DOMDataWriteTransaction writeTransaction, final LogicalDatastoreType datastore,
            InstanceIdentifier path) {
        LOG.info("Delete " + datastore.name() + " via Restconf: {}", path);
        writeTransaction.delete(datastore, path);
        return writeTransaction.commit();
    }

    public void setDomDataBroker(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }
}
