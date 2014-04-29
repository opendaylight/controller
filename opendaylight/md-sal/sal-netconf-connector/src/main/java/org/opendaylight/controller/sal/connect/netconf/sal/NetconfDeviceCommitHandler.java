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
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public final class NetconfDeviceCommitHandler implements DataCommitHandler<InstanceIdentifier,CompositeNode> {

    private final InstanceIdentifier path;
    private final RpcImplementation rpc;
    private boolean rollbackSupported = false;

    public NetconfDeviceCommitHandler(final InstanceIdentifier path, final RpcImplementation rpc, final boolean rollbackSupported) {
        this.path = path;
        this.rpc = rpc;
        this.rollbackSupported = rollbackSupported;
    }

    @Override
    public DataCommitTransaction<InstanceIdentifier, CompositeNode> requestCommit(
            final DataModification<InstanceIdentifier, CompositeNode> modification) {

        final NetconfDeviceTwoPhaseCommitTransaction twoPhaseCommit = new NetconfDeviceTwoPhaseCommitTransaction(rpc,
                modification, true, rollbackSupported);
        try {
            twoPhaseCommit.prepare();
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for response", e);
        } catch (final ExecutionException e) {
            throw new RuntimeException("Read configuration data " + path + " failed", e);
        }
        return twoPhaseCommit;
    }
}
