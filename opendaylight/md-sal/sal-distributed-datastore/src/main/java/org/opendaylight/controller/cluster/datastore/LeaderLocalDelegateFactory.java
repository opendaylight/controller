/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

/**
 * Base class for factories instantiating delegates which are local to the
 * shard leader.
 *
 * <D> delegate type
 * <M> message type
 */
abstract class LeaderLocalDelegateFactory<M, D> extends DelegateFactory<M, D> {
    abstract void onLeadershipChange(boolean amLeaderNow);
    abstract void onMessage(M message, boolean isLeader);
}
