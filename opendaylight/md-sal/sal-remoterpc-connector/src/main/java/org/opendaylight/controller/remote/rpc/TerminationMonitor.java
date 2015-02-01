/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.Terminated;
import akka.actor.UntypedActor;
import org.opendaylight.controller.cluster.common.actor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationMonitor extends UntypedActor{
    private static final Logger LOG = LoggerFactory.getLogger(TerminationMonitor.class);

    public TerminationMonitor(){
        LOG.debug("Created TerminationMonitor");
    }

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof Terminated){
            Terminated terminated = (Terminated) message;
            if(LOG.isDebugEnabled()) {
                LOG.debug("Actor terminated : {}", terminated.actor());
            }
        }else if(message instanceof Monitor){
          Monitor monitor = (Monitor) message;
          getContext().watch(monitor.getActorRef());
        }
    }
}
