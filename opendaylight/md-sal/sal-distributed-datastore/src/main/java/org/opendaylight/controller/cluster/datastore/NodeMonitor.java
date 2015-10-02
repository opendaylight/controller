/*
 * Copyright (c) 2015 Huawei Technologies Co., Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.UntypedActor;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.remote.AssociationErrorEvent;
import akka.remote.InvalidAssociation;
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
public class NodeMonitor extends UntypedActor {

    private final Logger LOG = LoggerFactory.getLogger(NodeMonitor.class);

    public static final String ADDRESS = "node-monitor";

    public NodeMonitor() {
        LOG.debug("Created NodeMonitor");
    }

    @Override
    public void postStop() {
        LOG.debug("Stopping NodeMonitor");
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        LOG.trace("onReceive {} {}", messageType, message);
        if (message instanceof ClusterEvent.MemberUp) {
            memberUp((ClusterEvent.MemberUp) message);
        } else if (message instanceof ClusterEvent.MemberRemoved) {
            memberRemoved((ClusterEvent.MemberRemoved) message);
        } else if (message instanceof ClusterEvent.UnreachableMember) {
            memberUnreachable((ClusterEvent.UnreachableMember) message);
        } else {

            // check to see if we got quarantined by another node
            if (message instanceof AssociationErrorEvent) {
                AssociationErrorEvent event = (AssociationErrorEvent) message;
                Throwable cause = event.getCause();
                if (cause instanceof InvalidAssociation) {
                    Throwable cause2 = ((InvalidAssociation) cause).getCause();
                    if (cause2.getMessage().contains("quarantined this system")) {
                        LOG.warn("Got quarantined by {}", event.getRemoteAddress());
                        DistributedDataStoreFactory.restartActorSystem();
                    } else {
                        LOG.debug("received AssociationErrorEvent, cause: InvalidAssociation", cause2);
                    }
                } else {
                    LOG.warn("received AssociationErrorEvent", cause);
                }
            }
        }
    }

    private void memberUp(MemberUp message) {
        Member member = message.member();
        String memberName = member.roles().head();
        LOG.info("MemberUp {}", memberName);
    }

    private void memberRemoved(MemberRemoved message) {
        Member member = message.member();
        String memberName = member.roles().head();
        LOG.warn("MemberRemoved {}", memberName);
    }

    private void memberUnreachable(UnreachableMember message) {
        Member member = message.member();
        String memberName = member.roles().head();
        LOG.info("UnreachableMember {}", memberName);
    }

}
