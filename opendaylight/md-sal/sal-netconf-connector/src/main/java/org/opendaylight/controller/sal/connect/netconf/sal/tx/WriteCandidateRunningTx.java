/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfRpcFutureCallback;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tx implementation for netconf devices that support only candidate datastore and writable running
 * The sequence goes exactly as with only candidate supported, with one addition:
 * <ul>
 *     <li>Running datastore is locked as the first thing and this lock has to succeed</li>
 * </ul>
 */
public class WriteCandidateRunningTx extends WriteCandidateTx {

    private static final Logger LOG  = LoggerFactory.getLogger(WriteCandidateRunningTx.class);

    public WriteCandidateRunningTx(final RemoteDeviceId id, final NetconfBaseOps netOps, final DataNormalizer normalizer, final NetconfSessionPreferences netconfSessionPreferences) {
        super(id, netOps, normalizer, netconfSessionPreferences);
    }

    @Override
    protected synchronized void init() {
        lockRunning();
        super.init();
    }

    @Override
    protected void cleanupOnSuccess() {
        super.cleanupOnSuccess();
        unlockRunning();
    }

    private void lockRunning() {
        try {
            invokeBlocking("Lock running", new Function<NetconfBaseOps, ListenableFuture<RpcResult<CompositeNode>>>() {
                @Override
                public ListenableFuture<RpcResult<CompositeNode>> apply(final NetconfBaseOps input) {
                    return input.lockRunning(new NetconfRpcFutureCallback("Lock running", id));
                }
            });
        } catch (final NetconfDocumentedException e) {
            LOG.warn("{}: Failed to lock running. Failed to initialize transaction", e);
            finished = true;
            throw new RuntimeException(id + ": Failed to lock running. Failed to initialize transaction", e);
        }
    }

    /**
     * This has to be non blocking since it is called from a callback on commit and its netty threadpool that is really sensitive to blocking calls
     */
    private void unlockRunning() {
        netOps.unlockRunning(new NetconfRpcFutureCallback("Unlock running", id));
    }
}
