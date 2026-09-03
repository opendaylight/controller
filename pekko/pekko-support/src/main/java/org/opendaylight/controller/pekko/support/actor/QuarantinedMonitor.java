/*
 * Copyright (c) 2015 Huawei Technologies Co., Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.actor;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.ClusterEvent;
import org.apache.pekko.remote.AssociationErrorEvent;
import org.apache.pekko.remote.RemotingLifecycleEvent;
import org.apache.pekko.remote.artery.ThisActorSystemQuarantinedEvent;
import org.opendaylight.controller.pekko.support.spi.DefaultActorSystemInstance.Callbacks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to Akka RemotingLifecycleEvent events to detect when this node has been
 * quarantined by another. Once this node gets quarantined, restart the ActorSystem to allow this
 * node to rejoin the cluster.
 *
 * @author Gary Wu gary.wu1@huawei.com
 */
public final class QuarantinedMonitor extends UntypedAbstractActor {
    public static final String ADDRESS = "quarantined-monitor";

    private static final Logger LOG = LoggerFactory.getLogger(QuarantinedMonitor.class);
    private static final Integer MESSAGE_THRESHOLD = 10;

    private final HashSet<Address> addressSet = new HashSet<>();
    private final Callbacks callbacks;

    private boolean quarantined;
    private int count = 0;

    QuarantinedMonitor(final Callbacks callbacks) {
        this.callbacks = requireNonNull(callbacks);

        LOG.debug("Created QuarantinedMonitorActor");

        getContext().system().eventStream().subscribe(self(), RemotingLifecycleEvent.class);
        getContext().system().eventStream().subscribe(self(), ClusterEvent.MemberDowned.class);
    }

    public static Props props(final Callbacks callbacks) {
        return Props.create(QuarantinedMonitor.class, callbacks);
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

        switch (message) {
            case ThisActorSystemQuarantinedEvent event -> {
                final var remoteAddress = event.remoteAddress();
                LOG.warn("Got quarantined by {}", remoteAddress);
                quarantined = true;
                callbacks.onRemoteQuarantined(remoteAddress.address());
            }
            case AssociationErrorEvent event -> {
                final String errorMessage = message.toString();
                LOG.trace("errorMessage:{}", errorMessage);
                if (errorMessage.contains("The remote system has a UID that has been quarantined")) {
                    final Address address = event.getRemoteAddress();
                    addressSet.add(address);
                    count++;
                    LOG.trace("address:{} addressSet: {} count:{}", address, addressSet, count);
                    if (count >= MESSAGE_THRESHOLD && addressSet.size() > 1) {
                        final var remoteAddress = event.remoteAddress();
                        count = 0;
                        addressSet.clear();
                        LOG.warn("Got quarantined via AssociationEvent by {}", remoteAddress);
                        quarantined = true;
                        // execute the callback
                        callbacks.onRemoteQuarantined(remoteAddress);
                    }
                } else if (errorMessage.contains("The remote system explicitly disassociated")) {
                    count = 0;
                    addressSet.clear();
                }
            }
            case ClusterEvent.MemberDowned event -> {
                if (Cluster.get(getContext().system()).selfMember().equals(event.member())) {
                    LOG.warn("This member has been downed, restarting");
                    callbacks.onLocalDown();
                }
            }
            case null, default -> {
                // no-op
            }
        }
    }
}
