/*
 * Copyright (c) 2015 Huawei Technologies Co., Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.japi.Effect;
import akka.remote.AssociationErrorEvent;
import akka.remote.RemotingLifecycleEvent;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to Akka RemotingLifecycleEvent events to detect when this node has been
 * quarantined by another. Once this node gets quarantined, restart the ActorSystem to allow this
 * node to rejoin the cluster.
 *
 * @author Gary Wu gary.wu1@huawei.com
 *
 */
public class QuarantinedMonitorActor extends UntypedAbstractActor {

    private static final Logger LOG = LoggerFactory.getLogger(QuarantinedMonitorActor.class);

    public static final String ADDRESS = "quarantined-monitor";

    private final Effect callback;
    private boolean quarantined;

    private long count = 0;
    private Set<Address> addressSet = new HashSet<>();

    protected QuarantinedMonitorActor(final Effect callback) {
        this.callback = callback;

        LOG.debug("Created QuarantinedMonitorActor");

        getContext().system().eventStream().subscribe(getSelf(), RemotingLifecycleEvent.class);
    }

    @Override
    public void postStop() {
        LOG.debug("Stopping QuarantinedMonitorActor");
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        LOG.trace("onReceive {} {}", messageType, message);
        final String memberQuarantine = "The remote system has a UID that has been quarantined";
        final String memberShutdown = "The remote system explicitly disassociated";
        //limit count of quarantine message
        final long quarantineMessageCount = 10;
        //limit count of unreachable member address in quarantine message
        final long quarantineAddressCount = 1;

        // check to see if we got quarantined by another node
        if (quarantined) {
            return;
        }
        if (message instanceof AssociationErrorEvent) {
            String errorMessage = message.toString();
            if (errorMessage.contains(memberQuarantine)) {
                Address address = ((AssociationErrorEvent) message).getRemoteAddress();
                addressSet.add(address);
                count = count + 1;
                if (count >= quarantineMessageCount && addressSet.size() > quarantineAddressCount) {
                    // count of quarantine message and unreachable member address has all
                    // exceeded limit,and know we have got quarantined by another node, restart myself.
                    final AssociationErrorEvent event = (AssociationErrorEvent) message;
                    LOG.warn("Got quarantined by {}", event.remoteAddress());
                    quarantined = true;

                    // execute the callback
                    callback.apply();
                }
            } else if (errorMessage.contains(memberShutdown)) {
                // receive the shutdown message of isolated node, clear the statistical count.
                // This step is to handle the count of unquarantined members and prevent them from restarting.
                count = 0L;
                addressSet.clear();
            }
        }
    }

    public static Props props(final Effect callback) {
        return Props.create(QuarantinedMonitorActor.class, callback);
    }
}
