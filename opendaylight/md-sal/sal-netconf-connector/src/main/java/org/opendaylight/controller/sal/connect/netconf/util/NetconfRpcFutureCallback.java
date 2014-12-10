/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.util;

import com.google.common.util.concurrent.FutureCallback;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.WriteRunningTx;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Netconf rpc logging callback
 */
public class NetconfRpcFutureCallback implements FutureCallback<RpcResult<CompositeNode>> {
    private static final Logger LOG  = LoggerFactory.getLogger(WriteRunningTx.class);

    private final String type;
    private final RemoteDeviceId id;

    public NetconfRpcFutureCallback(final String prefix, final RemoteDeviceId id) {
        this.type = prefix;
        this.id = id;
    }

    @Override
    public void onSuccess(final RpcResult<CompositeNode> result) {
        if(result.isSuccessful()) {
            LOG.trace("{}: " + type + " invoked successfully", id);
        } else {
            onUnsuccess(result);
        }
    }

    protected void onUnsuccess(final RpcResult<CompositeNode> result) {
        LOG.warn("{}: " + type + " invoked unsuccessfully: {}", id, result.getErrors());
    }

    @Override
    public void onFailure(final Throwable t) {
        LOG.warn("{}: " + type + " failed.", id, t);
    }
}
