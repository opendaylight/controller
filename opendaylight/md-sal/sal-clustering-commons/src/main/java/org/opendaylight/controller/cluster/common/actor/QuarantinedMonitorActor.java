/*
 * Copyright (c) 2015 Huawei Technologies Co., Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import java.util.HashSet;
import java.util.Set;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.ClusterEvent;
import org.apache.pekko.japi.function.Effect;
import org.apache.pekko.remote.AssociationErrorEvent;
import org.apache.pekko.remote.RemotingLifecycleEvent;
import org.apache.pekko.remote.artery.ThisActorSystemQuarantinedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to Akka RemotingLifecycleEvent events to detect when this node has been
 * quarantined by another. Once this node gets quarantined, restart the ActorSystem to allow this
 * node to rejoin the cluster.
 *
 * @author Gary Wu gary.wu1@huawei.com
 */
public final class QuarantinedMonitorActor extends UntypedAbstractActor {
    public static final String ADDRESS = "quarantined-monitor";

    private static final Logger LOG = LoggerFactory.getLogger(QuarantinedMonitorActor.class);
    private static final Integer MESSAGE_THRESHOLD = 10;

    private final Effect callback;
    private boolean quarantined;

    private final Set<Address> addressSet = new HashSet<>();
    private int count = 0;

    protected QuarantinedMonitorActor(final Effect callback) {
        this.callback = callback;

        LOG.debug("Created QuarantinedMonitorActor");

        getContext().system().eventStream().subscribe(self(), RemotingLifecycleEvent.class);
        getContext().system().eventStream().subscribe(self(), ClusterEvent.MemberDowned.class);
    }

    @Override
    public void postStop() {
        LOG.debug("Stopping QuarantinedMonitorActor");
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        LOG.trace("onReceive {} {}", messageType, message);

        // check to see if we got quarantined by another node
        if (quarantined) {
            return;
        }

        if (message instanceof ThisActorSystemQuarantinedEvent event) {
            LOG.warn("Got quarantined by {}", event.remoteAddress());
            quarantined = true;

            // execute the callback
            callback.apply();
        } else if (message instanceof AssociationErrorEvent event) {
            final String errorMessage = message.toString();
            LOG.trace("errorMessage:{}", errorMessage);
            if (errorMessage.contains("The remote system has a UID that has been quarantined")) {
                final Address address = event.getRemoteAddress();
                addressSet.add(address);
                count++;
                LOG.trace("address:{} addressSet: {} count:{}", address, addressSet, count);
                if (count >= MESSAGE_THRESHOLD && addressSet.size() > 1) {
                    count = 0;
                    addressSet.clear();
                    LOG.warn("Got quarantined via AssociationEvent by {}", event.remoteAddress());
                    quarantined = true;

                    // execute the callback
                    callback.apply();
                }
            } else if (errorMessage.contains("The remote system explicitly disassociated")) {
                count = 0;
                addressSet.clear();
            }
        } else if (message instanceof ClusterEvent.MemberDowned event) {
            if (Cluster.get(getContext().system()).selfMember().equals(event.member())) {
                LOG.warn("This member has been downed, restarting");

                callback.apply();
            }
        }
    }

    public static Props props(final Effect callback) {
        return Props.create(QuarantinedMonitorActor.class, callback);
    }
}
