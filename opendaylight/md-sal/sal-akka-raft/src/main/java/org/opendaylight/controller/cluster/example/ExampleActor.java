/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example;

import org.opendaylight.controller.cluster.raft.RaftActor;

/**
 * A sample actor showing how the RaftActor is to be extended
 */
public class ExampleActor extends RaftActor {
    public ExampleActor(String id) {
        super(id);
    }

    @Override public void onReceiveCommand(Object message){
        /*
            Here the extended class does whatever it needs to do.
            If it cannot handle a message then it passes it on to the super
            class for handling
         */
        super.onReceiveCommand(message);
    }

    @Override public void onReceiveRecover(Object message) {
        super.onReceiveRecover(message);
    }

}
