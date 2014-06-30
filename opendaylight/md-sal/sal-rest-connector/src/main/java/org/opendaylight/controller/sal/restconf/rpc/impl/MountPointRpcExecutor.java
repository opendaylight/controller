/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.rpc.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Provides an implementation which invokes rpc methods via a mounted yang data model.
 *
 * @author Devin Avery
 *
 */
public class MountPointRpcExecutor extends AbstractRpcExecutor {
    private final DOMMountPoint mountPoint;

    public MountPointRpcExecutor(RpcDefinition rpcDef, DOMMountPoint mountPoint) {
        super(rpcDef);
        this.mountPoint = mountPoint;
        Preconditions.checkNotNull(mountPoint, "MountInstance can not be null.");
    }

    @Override
    protected Future<RpcResult<CompositeNode>> invokeRpcUnchecked(CompositeNode rpcRequest) {
        Optional<RpcProvisionRegistry> service = mountPoint.getService(RpcProvisionRegistry.class);
        if (service.isPresent()) {
            return service.get().invokeRpc(getRpcDefinition().getQName(), rpcRequest);
        }
        throw new RestconfDocumentedException("Rpc service is missing.");
    }
}