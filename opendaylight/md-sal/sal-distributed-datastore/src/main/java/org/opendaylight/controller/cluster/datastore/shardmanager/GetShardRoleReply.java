/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Reply to GetShardRole, containing the current role of the shard if present on the ShardManager.
 */
public record GetShardRoleReply(@Nullable String role) {
    // Nothing else
}
