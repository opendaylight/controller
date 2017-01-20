/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

/**
 * Blueprint bean corresponding to the "action-provider" element that registers the promise to instantiate action
 * instances with RpcProviderRegistry.
 *
 * @author Robert Varga
 */
final class ActionProviderBean extends AbstractInvokableImplementationBean {
    static final String ACTION_PROVIDER = "action-provider";

    @Override
    String logName() {
        return ACTION_PROVIDER;
    }
}
