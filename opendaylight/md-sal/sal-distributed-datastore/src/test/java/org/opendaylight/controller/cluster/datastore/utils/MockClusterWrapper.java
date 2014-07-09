/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;

public class MockClusterWrapper implements ClusterWrapper{

    @Override public void subscribeToMemberEvents(ActorRef actorRef) {
        throw new UnsupportedOperationException("subscribeToMemberEvents");
    }

    @Override public String getCurrentMemberName() {
        return "member-1";
    }
}
