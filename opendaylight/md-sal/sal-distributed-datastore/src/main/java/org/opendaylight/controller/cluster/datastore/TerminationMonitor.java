/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.opendaylight.controller.cluster.datastore.messages.Monitor;

public class TerminationMonitor extends UntypedActor{
    protected final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);

    public TerminationMonitor(){
        LOG.info("Created TerminationMonitor");
    }

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof Terminated){
            Terminated terminated = (Terminated) message;
            LOG.debug("Actor terminated : {}", terminated.actor());
        } else if(message instanceof Monitor){
            Monitor monitor = (Monitor) message;
            getContext().watch(monitor.getActorRef());
        }
    }
}
