/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

/**
 * Blueprint bean corresponding to the "rpc-implementation" element that registers an RPC implementation with
 * the RpcProviderRegistry.
 *
 * @author Thomas Pantelis
 */
public class RpcImplementationBean extends AbstractInvokableImplementationBean {
    static final String RPC_IMPLEMENTATION = "rpc-implementation";

    @Override
    String logName() {
        return RPC_IMPLEMENTATION;
    }
}
