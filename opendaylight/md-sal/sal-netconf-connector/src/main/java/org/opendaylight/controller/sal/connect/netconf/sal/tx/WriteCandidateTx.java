/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfRpcFutureCallback;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tx implementation for netconf devices that support only candidate datastore and no writable running
 * The sequence goes as:
 * <ol>
 * <li/> Lock candidate datastore on tx construction
 *  <ul>
 * <li/> Lock has to succeed, if it does not, an attempt to discard changes is made
 * <li/> Discard changes has to succeed
 * <li/> If discard is successful, lock is reattempted
 * <li/> Second lock attempt has to succeed
 * </ul>
 * <li/> Edit-config in candidate N times
 * <ul>
 * <li/> If any issue occurs during edit, datastore is discarded using discard-changes rpc, unlocked and an exception is thrown async
 * </ul>
 * <li/> Commit and Unlock candidate datastore async
 * </ol>
 */
public class WriteCandidateTx extends AbstractWriteTx {

    private static final Logger LOG  = LoggerFactory.getLogger(WriteCandidateTx.class);

    private static final Function<RpcResult<CompositeNode>, RpcResult<TransactionStatus>> RPC_RESULT_TO_TX_STATUS = new Function<RpcResult<CompositeNode>, RpcResult<TransactionStatus>>() {
        @Override
        public RpcResult<TransactionStatus> apply(final RpcResult<CompositeNode> input) {
            if (input.isSuccessful()) {
                return RpcResultBuilder.success(TransactionStatus.COMMITED).build();
            } else {
                final RpcResultBuilder<TransactionStatus> failed = RpcResultBuilder.failed();
                for (final RpcError rpcError : input.getErrors()) {
                    failed.withError(rpcError.getErrorType(), rpcError.getTag(), rpcError.getMessage(),
                            rpcError.getApplicationTag(), rpcError.getInfo(), rpcError.getCause());
                }
                return failed.build();
            }
        }
    };

    public WriteCandidateTx(final RemoteDeviceId id, final NetconfBaseOps rpc, final DataNormalizer normalizer, final NetconfSessionPreferences netconfSessionPreferences) {
        super(rpc, id, normalizer, netconfSessionPreferences);
    }

    @Override
    protected synchronized void init() {
        LOG.trace("{}: Initializing {} transaction", id, getClass().getSimpleName());

        try {
            lock();
        } catch (final NetconfDocumentedException e) {
            try {
                LOG.warn("{}: Failed to lock candidate, attempting discard changes", id);
                discardChanges();
                LOG.warn("{}: Changes discarded successfully, attempting lock", id);
                lock();
            } catch (final NetconfDocumentedException secondE) {
                LOG.error("{}: Failed to prepare candidate. Failed to initialize transaction", id, secondE);
                throw new RuntimeException(id + ": Failed to prepare candidate. Failed to initialize transaction", secondE);
            }
        }
    }

    private void lock() throws NetconfDocumentedException {
        try {
            invokeBlocking("Lock candidate", new Function<NetconfBaseOps, ListenableFuture<RpcResult<CompositeNode>>>() {
                @Override
                public ListenableFuture<RpcResult<CompositeNode>> apply(final NetconfBaseOps input) {
                    return input.lockCandidate(new NetconfRpcFutureCallback("Lock candidate", id));
                }
            });
        } catch (final NetconfDocumentedException e) {
            LOG.warn("{}: Failed to lock candidate", id, e);
            throw e;
        }
    }

    @Override
    protected void cleanup() {
        discardChanges();
        cleanupOnSuccess();
    }

    @Override
    protected void handleEditException(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data, final NetconfDocumentedException e, final String editType) {
        LOG.warn("{}: Error " + editType + " data to (candidate){}, data: {}, canceling", id, path, data, e);
        cancel();
        throw new RuntimeException(id + ": Error while " + editType + ": (candidate)" + path, e);
    }

    @Override
    protected void handleDeleteException(final YangInstanceIdentifier path, final NetconfDocumentedException e) {
        LOG.warn("{}: Error deleting data (candidate){}, canceling", id, path, e);
        cancel();
        throw new RuntimeException(id + ": Error while deleting (candidate)" + path, e);
    }

    @Override
    public synchronized CheckedFuture<Void, TransactionCommitFailedException> submit() {
        final ListenableFuture<Void> commitFutureAsVoid = Futures.transform(commit(), new Function<RpcResult<TransactionStatus>, Void>() {
            @Override
            public Void apply(final RpcResult<TransactionStatus> input) {
                return null;
            }
        });

        return Futures.makeChecked(commitFutureAsVoid, new Function<Exception, TransactionCommitFailedException>() {
            @Override
            public TransactionCommitFailedException apply(final Exception input) {
                return new TransactionCommitFailedException("Submit of transaction " + getIdentifier() + " failed", input);
            }
        });
    }

    /**
     * This has to be non blocking since it is called from a callback on commit and its netty threadpool that is really sensitive to blocking calls
     */
    private void discardChanges() {
        netOps.discardChanges(new NetconfRpcFutureCallback("Discarding candidate", id));
    }

    @Override
    public synchronized ListenableFuture<RpcResult<TransactionStatus>> performCommit() {
        final ListenableFuture<RpcResult<CompositeNode>> rpcResult = netOps.commit(new NetconfRpcFutureCallback("Commit", id) {
            @Override
            public void onSuccess(final RpcResult<CompositeNode> result) {
                super.onSuccess(result);
                LOG.debug("{}: Write successful, transaction: {}. Unlocking", id, getIdentifier());
                cleanupOnSuccess();
            }

            @Override
            protected void onUnsuccess(final RpcResult<CompositeNode> result) {
                LOG.error("{}: Write failed, transaction {}, discarding changes, unlocking: {}", id, getIdentifier(), result.getErrors());
                cleanup();
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("{}: Write failed, transaction {}, discarding changes, unlocking", id, getIdentifier(), t);
                cleanup();
            }
        });

        return Futures.transform(rpcResult, RPC_RESULT_TO_TX_STATUS);
    }

    protected void cleanupOnSuccess() {
        unlock();
    }

    @Override
    protected void editConfig(final CompositeNode editStructure, final Optional<ModifyAction> defaultOperation) throws NetconfDocumentedException {
        invokeBlocking("Edit candidate", new Function<NetconfBaseOps, ListenableFuture<RpcResult<CompositeNode>>>() {
            @Override
            public ListenableFuture<RpcResult<CompositeNode>> apply(final NetconfBaseOps input) {
                        return defaultOperation.isPresent()
                                ? input.editConfigCandidate(new NetconfRpcFutureCallback("Edit candidate", id), editStructure, defaultOperation.get(),
                                netconfSessionPreferences.isRollbackSupported())
                                : input.editConfigCandidate(new NetconfRpcFutureCallback("Edit candidate", id), editStructure,
                                netconfSessionPreferences.isRollbackSupported());
            }
        });
    }

    /**
     * This has to be non blocking since it is called from a callback on commit and its netty threadpool that is really sensitive to blocking calls
     */
    private void unlock() {
        netOps.unlockCandidate(new NetconfRpcFutureCallback("Unlock candidate", id));
    }

}
