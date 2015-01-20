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
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tx implementation for netconf devices that support only writable-running with no candidate
 * The sequence goes as:
 * <ol>
 * <li/> Lock running datastore on tx construction
 * <ul>
 * <li/> Lock has to succeed, if it does not, transaction is failed
 * </ul>
 * <li/> Edit-config in running N times
 * <ul>
 * <li/> If any issue occurs during edit, datastore is unlocked and an exception is thrown
 * </ul>
 * <li/> Unlock running datastore on tx commit
 * </ol>
 */
public class WriteRunningTx extends AbstractWriteTx {

    private static final Logger LOG  = LoggerFactory.getLogger(WriteRunningTx.class);

    public WriteRunningTx(final RemoteDeviceId id, final NetconfBaseOps netOps,
                          final DataNormalizer normalizer, final NetconfSessionPreferences netconfSessionPreferences) {
        super(netOps, id, normalizer, netconfSessionPreferences);
    }

    @Override
    protected synchronized void init() {
        lock();
    }

    private void lock() {
        try {
            invokeBlocking("Lock running", new Function<NetconfBaseOps, ListenableFuture<RpcResult<CompositeNode>>>() {
                @Override
                public ListenableFuture<RpcResult<CompositeNode>> apply(final NetconfBaseOps input) {
                    return input.lockRunning(new NetconfRpcFutureCallback("Lock running", id));
                }
            });
        } catch (final NetconfDocumentedException e) {
            LOG.warn("{}: Failed to initialize netconf transaction (lock running)", e);
            finished = true;
            throw new RuntimeException(id + ": Failed to initialize netconf transaction (lock running)", e);
        }
    }

    @Override
    protected void cleanup() {
        unlock();
    }

    @Override
    protected void handleEditException(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data, final NetconfDocumentedException e, final String editType) {
        LOG.warn("{}: Error " + editType + " data to (running){}, data: {}, canceling", id, path, data, e);
        cancel();
        throw new RuntimeException(id + ": Error while " + editType + ": (running)" + path, e);
    }

    @Override
    protected void handleDeleteException(final YangInstanceIdentifier path, final NetconfDocumentedException e) {
        LOG.warn("{}: Error deleting data (running){}, canceling", id, path, e);
        cancel();
        throw new RuntimeException(id + ": Error while deleting (running)" + path, e);
    }

    @Override
    public synchronized CheckedFuture<Void, TransactionCommitFailedException> submit() {
        final ListenableFuture<Void> commmitFutureAsVoid = Futures.transform(commit(), new Function<RpcResult<TransactionStatus>, Void>() {
            @Override
            public Void apply(final RpcResult<TransactionStatus> input) {
                return null;
            }
        });

        return Futures.makeChecked(commmitFutureAsVoid, new Function<Exception, TransactionCommitFailedException>() {
            @Override
            public TransactionCommitFailedException apply(final Exception input) {
                return new TransactionCommitFailedException("Submit of transaction " + getIdentifier() + " failed", input);
            }
        });
    }

    @Override
    public synchronized ListenableFuture<RpcResult<TransactionStatus>> performCommit() {
        unlock();
        return Futures.immediateFuture(RpcResultBuilder.success(TransactionStatus.COMMITED).build());
    }

    @Override
    protected void editConfig(final CompositeNode editStructure, final Optional<ModifyAction> defaultOperation) throws NetconfDocumentedException {
        invokeBlocking("Edit running", new Function<NetconfBaseOps, ListenableFuture<RpcResult<CompositeNode>>>() {
            @Override
            public ListenableFuture<RpcResult<CompositeNode>> apply(final NetconfBaseOps input) {
                        return defaultOperation.isPresent()
                                ? input.editConfigRunning(new NetconfRpcFutureCallback("Edit running", id), editStructure, defaultOperation.get(),
                                netconfSessionPreferences.isRollbackSupported())
                                : input.editConfigRunning(new NetconfRpcFutureCallback("Edit running", id), editStructure,
                                netconfSessionPreferences.isRollbackSupported());
            }
        });
    }

    private void unlock() {
        try {
            invokeBlocking("Unlocking running", new Function<NetconfBaseOps, ListenableFuture<RpcResult<CompositeNode>>>() {
                @Override
                public ListenableFuture<RpcResult<CompositeNode>> apply(final NetconfBaseOps input) {
                    return input.unlockRunning(new NetconfRpcFutureCallback("Unlock running", id));
                }
            });
        } catch (final NetconfDocumentedException e) {
            LOG.warn("{}: Failed to unlock running datastore", e);
            throw new RuntimeException(id + ": Failed to unlock running datastore", e);
        }
    }
}
