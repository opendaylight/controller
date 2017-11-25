/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;

public final class ForwardingActor extends UntypedAbstractActor {
    private final ActorRef target;

    private ForwardingActor(final ActorRef target) {
        this.target = target;
    }

    @Override
    public void onReceive(final Object obj) throws Exception {
        target.forward(obj, context());
    }

}
