/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfDeviceCommitHandler implements DataCommitHandler<InstanceIdentifier,CompositeNode> {

    private static final Logger logger= LoggerFactory.getLogger(NetconfDeviceCommitHandler.class);

    private final RemoteDeviceId id;
    private final RpcImplementation rpc;
    private final boolean rollbackSupported;

    public NetconfDeviceCommitHandler(final RemoteDeviceId id, final RpcImplementation rpc, final boolean rollbackSupported) {
        this.id = id;
        this.rpc = rpc;
        this.rollbackSupported = rollbackSupported;
    }

    @Override
    public DataCommitTransaction<InstanceIdentifier, CompositeNode> requestCommit(
            final DataModification<InstanceIdentifier, CompositeNode> modification) {

        final NetconfDeviceTwoPhaseCommitTransaction twoPhaseCommit = new NetconfDeviceTwoPhaseCommitTransaction(id, rpc,
                modification, true, rollbackSupported);
        try {
            twoPhaseCommit.prepare();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(id + ": Interrupted while waiting for response", e);
        } catch (final ExecutionException e) {
            logger.warn("{}: Error executing pre commit operation on remote device", id, e);
            return new FailingTransaction(twoPhaseCommit, e);
        }

        return twoPhaseCommit;
    }

    /**
     * Always fail commit transaction that rolls back delegate transaction afterwards
     */
    private class FailingTransaction implements DataCommitTransaction<InstanceIdentifier, CompositeNode> {
        private final NetconfDeviceTwoPhaseCommitTransaction twoPhaseCommit;
        private final ExecutionException e;

        public FailingTransaction(final NetconfDeviceTwoPhaseCommitTransaction twoPhaseCommit, final ExecutionException e) {
            this.twoPhaseCommit = twoPhaseCommit;
            this.e = e;
        }

        @Override
        public DataModification<InstanceIdentifier, CompositeNode> getModification() {
            return twoPhaseCommit.getModification();
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            return RpcResultBuilder.<Void>failed().withError( RpcError.ErrorType.APPLICATION,
                    id + ": Unexpected operation error during pre-commit operations", e ).build();
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            return twoPhaseCommit.rollback();
        }
    }
}
