/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public final class GetAllBuckets {
    private static final GetAllBuckets INSTANCE = new GetAllBuckets();

    private GetAllBuckets() {

    }

    public static GetAllBuckets getInstance() {
        return INSTANCE;
    }
}