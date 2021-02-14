/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding.messages;

/**
 * Message sent to the local ShardManager, once the shard configuration shard is ready and the ShardManager should
 * start its listener.
 */
@Deprecated(forRemoval = true)
public final class InitConfigListener {

    public static final InitConfigListener INSTANCE = new InitConfigListener();

    private InitConfigListener() {

    }
}
