/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin.command;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ActivateEosDatacenter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ActivateEosDatacenterInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Service
@Command(scope = "cluster-admin", name = "activate-eos-datacenter", description = "Run an activate-eos-datacenter test")
public class ActivateEosDatacenterCommand extends AbstractRpcAction {
    @Reference
    private RpcService rpcService;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcService.getRpc(ActivateEosDatacenter.class)
            .invoke(new ActivateEosDatacenterInputBuilder().build());
    }
}
