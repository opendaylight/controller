/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.PeerInfoCache;
import org.opendaylight.controller.cluster.raft.base.messages.KeepAlive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * @author Thomas Pantelis
 */
public class KeepAliveActor extends UntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(KeepAliveActor.class);

    private final String leaderId;
    private final PeerInfoCache peerInfoCache;
    private final Supplier<ConfigParams> configParams;
    private final KeepAlive keepAlive;
    private Cancellable keepAliveSchedule;

    public KeepAliveActor(String leaderId, PeerInfoCache peerInfoCache, Supplier<ConfigParams> configParams) {
        this.leaderId = leaderId;
        this.peerInfoCache = peerInfoCache;
        this.configParams = configParams;

        keepAlive = new KeepAlive(leaderId);

        scheduleKeepAlive();

        LOG.debug("{}: KeepAliveActor created", leaderId);
    }

    private void scheduleKeepAlive() {
        FiniteDuration interval = configParams.get().getKeepAliveInterval();
        keepAliveSchedule = context().system().scheduler().scheduleOnce(
                interval, self(), SendKeepAlive.INSTANCE, context().dispatcher(), self());
    }

    @Override
    public void postStop() {
        if(keepAliveSchedule != null) {
            keepAliveSchedule.cancel();
        }
    }

    @Override
    public void onReceive(Object message) {
        if(message instanceof SendKeepAlive) {
            String[] peerAddresses = peerInfoCache.getPeerAddresses();

            LOG.trace("{}: Received SendKeepAlive - peerAddresses: {}", leaderId, peerAddresses);

            for(String peerAddress: peerAddresses) {
                if(peerAddress != null) {
                    context().actorSelection(peerAddress).tell(keepAlive, self());
                }
            }

            scheduleKeepAlive();
        }
    }

    public static Props props(@Nonnull String logContext, @Nonnull PeerInfoCache peerInfoCache,
            @Nonnull Supplier<ConfigParams> configParams) {
        return Props.create(KeepAliveActor.class, () -> new KeepAliveActor(logContext, peerInfoCache, configParams));
    }

    private static class SendKeepAlive {
        static final SendKeepAlive INSTANCE = new SendKeepAlive();

        private SendKeepAlive() {
            // Hidden on purpose
        }
    }
}
