/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.impl;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.rest.RestBrokerFacade;
import org.opendaylight.controller.md.sal.rest.RestConnectorProviderImpl;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.impl
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 22, 2015
 */
public class RestBrokerFacadeImpl implements RestBrokerFacade {

    private static final Logger LOG = LoggerFactory.getLogger(RestBrokerFacadeImpl.class);

    private final DOMDataBroker domDataBroker;
    private final DOMRpcService domRpcService;

    public RestBrokerFacadeImpl(final DOMDataBroker domDataBroker, final DOMRpcService domRpcService) {
        this.domDataBroker = Preconditions.checkNotNull(domDataBroker);
        this.domRpcService = Preconditions.checkNotNull(domRpcService);
    }

    // READ configuration
    @Override
    public NormalizedNode<?, ?> readConfigurationData(final YangInstanceIdentifier path) {
        return readDataViaTransaction(domDataBroker.newReadOnlyTransaction(), CONFIGURATION, path);
    }

    @Override
    public NormalizedNode<?, ?> readConfigurationData(final DOMMountPoint mountPoint, final YangInstanceIdentifier path) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            return readDataViaTransaction(domDataBrokerService.get().newReadOnlyTransaction(), CONFIGURATION, path);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // READ operational
    @Override
    public NormalizedNode<?, ?> readOperationalData(final YangInstanceIdentifier path) {
        return readDataViaTransaction(domDataBroker.newReadOnlyTransaction(), OPERATIONAL, path);
    }

    @Override
    public NormalizedNode<?, ?> readOperationalData(final DOMMountPoint mountPoint, final YangInstanceIdentifier path) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            return readDataViaTransaction(domDataBrokerService.get().newReadOnlyTransaction(), OPERATIONAL, path);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // PUT configuration
    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        return putDataViaTransaction(domDataBroker.newReadWriteTransaction(), CONFIGURATION, path, payload, RestConnectorProviderImpl.getSchemaContext());
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPut(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            final SchemaContext schemaContext = mountPoint.getSchemaContext();
            return putDataViaTransaction(domDataBrokerService.get().newReadWriteTransaction(), CONFIGURATION, path, payload, schemaContext);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // POST configuration
    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        return postDataViaTransaction(domDataBroker.newReadWriteTransaction(), CONFIGURATION, path, payload, RestConnectorProviderImpl.getSchemaContext());
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataPost(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path, final NormalizedNode<?, ?> payload) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            final SchemaContext schemaContext = mountPoint.getSchemaContext();
            return postDataViaTransaction(domDataBrokerService.get().newReadWriteTransaction(), CONFIGURATION, path, payload, schemaContext);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // DELETE configuration
    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(
            final YangInstanceIdentifier path) {
        return deleteDataViaTransaction(domDataBroker.newWriteOnlyTransaction(), CONFIGURATION, path);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> commitConfigurationDataDelete(
            final DOMMountPoint mountPoint, final YangInstanceIdentifier path) {
        final Optional<DOMDataBroker> domDataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if (domDataBrokerService.isPresent()) {
            return deleteDataViaTransaction(domDataBrokerService.get().newWriteOnlyTransaction(), CONFIGURATION, path);
        }
        throw new RestconfDocumentedException("DOM data broker service isn't available for mount point.");
    }

    // RPC
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type, final NormalizedNode<?, ?> input) {
        return domRpcService.invokeRpc(type, input);
    }


    private static final NormalizedNode<?, ?> readDataViaTransaction(final DOMDataReadOnlyTransaction tx,
            final LogicalDatastoreType ds, final YangInstanceIdentifier path) {
        LOG.trace("Read " + ds.name() + " via Restconf: {}", path);
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> listenableFuture = tx.read(ds, path);
        Optional<NormalizedNode<?, ?>> optional = Optional.absent();
        if (listenableFuture != null) {
            try {
                LOG.debug("Reading result data from transaction.");
                optional = listenableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RestconfDocumentedException("Problem to get data from transaction.", e.getCause());
            }
        }
        return optional.isPresent() ? optional.get() : null;
    }

    private static final CheckedFuture<Void, TransactionCommitFailedException> deleteDataViaTransaction(
            final DOMDataWriteTransaction tx, final LogicalDatastoreType ds, final YangInstanceIdentifier path) {
        LOG.trace("Delete " + ds.name() + " via Restconf: {}", path);
        tx.delete(ds, path);
        return tx.submit();
    }

    private static final CheckedFuture<Void, TransactionCommitFailedException> postDataViaTransaction(
            final DOMDataReadWriteTransaction tx, final LogicalDatastoreType ds, final YangInstanceIdentifier parentPath,
            final NormalizedNode<?, ?> payload, final SchemaContext context) {
        // FIXME: This is doing correct post for container and list children
        //        not sure if this will work for choice case
        final YangInstanceIdentifier path;
        if(payload instanceof MapEntryNode) {
            path = parentPath.node(payload.getNodeType()).node(payload.getIdentifier());
        } else {
            path = parentPath.node(payload.getIdentifier());
        }

        final ListenableFuture<Optional<NormalizedNode<?, ?>>> futureDatastoreData = tx.read(ds, path);
        try {
            final Optional<NormalizedNode<?, ?>> optionalDatastoreData = futureDatastoreData.get();
            if (optionalDatastoreData.isPresent()) {
                final String errMsg = "Post Configuration via Restconf was not executed because data already exists for path :{}" + path;
                LOG.trace(errMsg);
                tx.cancel();
                throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.DATA_EXISTS);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.trace("It wasn't possible to get data loaded from datastore at path " + path);
        }

        ensureParentsByMerge(ds, path, tx, context);
        tx.put(ds, path, payload);
        LOG.trace("Post " + ds.name() + " via Restconf: {}", path);
        return tx.submit();
    }

    private static final CheckedFuture<Void, TransactionCommitFailedException> putDataViaTransaction(
            final DOMDataReadWriteTransaction tx, final LogicalDatastoreType ds, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> payload, final SchemaContext context) {
        LOG.trace("Put " + ds.name() + " via Restconf: {}", path);
        ensureParentsByMerge(ds, path, tx, context);
        tx.put(ds, path, payload);
        return tx.submit();
    }

    private static final void ensureParentsByMerge(final LogicalDatastoreType ds, final YangInstanceIdentifier path,
            final DOMDataReadWriteTransaction tx, final SchemaContext context) {
        final List<PathArgument> currentArguments = new ArrayList<>();
        final Iterator<PathArgument> iterator = path.getPathArguments().iterator();
        // TODO find not deprecated alternative what could make a default not existing parent node too
        DataNormalizationOperation<?> currentOp = DataNormalizationOperation.from(context);
        while (iterator.hasNext()) {
            final PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (final DataNormalizationException e) {
                tx.cancel();
                throw new IllegalArgumentException(
                        String.format("Invalid child encountered in path %s", path), e);
            }
            currentArguments.add(currentArg);
            final YangInstanceIdentifier currentPath = YangInstanceIdentifier.create(currentArguments);

            final Boolean exists;

            try {

                final CheckedFuture<Boolean, ReadFailedException> future = tx.exists(ds, currentPath);
                exists = future.checkedGet();
            } catch (final ReadFailedException e) {
                LOG.error("Failed to read pre-existing data from store {} path {}", ds, currentPath, e);
                tx.cancel();
                throw new IllegalStateException("Failed to read pre-existing data", e);
            }

            if (!exists && iterator.hasNext()) {
                tx.merge(ds, currentPath, currentOp.createDefault(currentArg));
            }
        }
    }
}
