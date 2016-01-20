/*
 * Copyright (c) 2015 Huawei Technologies Co., Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.japi.Effect;
import akka.remote.AssociationErrorEvent;
import akka.remote.InvalidAssociation;
import akka.remote.RemotingLifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to Akka RemotingLifecycleEvent events to detect when this node has been
 * quarantined by another. Once this node gets quarantined, restart the ActorSystem to allow this
 * node to rejoin the cluster.
 *
 * @author Gary Wu <gary.wu1@huawei.com>
 *
 */
public class QuarantinedMonitorActor extends UntypedActor {

    private final Logger LOG = LoggerFactory.getLogger(QuarantinedMonitorActor.class);

    public static final String ADDRESS = "quarantined-monitor";

    private final Effect callback;

    protected QuarantinedMonitorActor(Effect callback) {
        this.callback = callback;

        LOG.debug("Created QuarantinedMonitorActor");

        getContext().system().eventStream().subscribe(getSelf(), RemotingLifecycleEvent.class);
    }

    @Override
    public void postStop() {
        LOG.debug("Stopping QuarantinedMonitorActor");
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        LOG.trace("onReceive {} {}", messageType, message);

        // check to see if we got quarantined by another node

        // TODO: follow https://github.com/akka/akka/issues/18758 to see if Akka adds a named
        // exception for quarantine detection
        if (message instanceof AssociationErrorEvent) {
            AssociationErrorEvent event = (AssociationErrorEvent) message;
            Throwable cause = event.getCause();
            if (cause instanceof InvalidAssociation) {
                Throwable cause2 = ((InvalidAssociation) cause).getCause();
                if (cause2.getMessage().contains("quarantined this system")) {
                    LOG.warn("Got quarantined by {}", event.getRemoteAddress());

                    // execute the callback
                    callback.apply();
                } else {
                    LOG.debug("received AssociationErrorEvent, cause: InvalidAssociation", cause2);
                }
            } else {
                LOG.debug("received AssociationErrorEvent", cause);
            }
        }
    }

    public static Props props(final Effect callback) {
        return Props.create(new Creator<QuarantinedMonitorActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public QuarantinedMonitorActor create() throws Exception {
                return new QuarantinedMonitorActor(callback);
            }
        });
    }

}
