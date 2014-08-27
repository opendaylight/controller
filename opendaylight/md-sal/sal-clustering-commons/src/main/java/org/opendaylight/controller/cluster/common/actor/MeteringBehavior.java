/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import akka.actor.UntypedActor;
import akka.japi.Procedure;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;

public class MeteringBehavior implements Procedure<Object> {

    private final UntypedActor meteredActor;

    private final MetricRegistry METRICREGISTRY = MetricsReporter.getInstance().getMetricsRegistry();
    private final String MSG_PROCESSING_RATE = "msg-rate";

    private String actorName;
    private Timer msgProcessingTimer;

    public MeteringBehavior(UntypedActor actor){
        Preconditions.checkArgument(actor != null, "actor must not be null");

        this.meteredActor = actor;
        actorName = meteredActor.getSelf().path().toStringWithoutAddress();
        final String msgProcessingTime = MetricRegistry.name(actorName, MSG_PROCESSING_RATE);
        msgProcessingTimer = METRICREGISTRY.timer(msgProcessingTime);
    }

    @Override
    public void apply(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();

        final String msgProcessingTimeByMsgType =
                MetricRegistry.name(actorName, MSG_PROCESSING_RATE, messageType);

        final Timer msgProcessingTimerByMsgType = METRICREGISTRY.timer(msgProcessingTimeByMsgType);

        //start timers
        final Timer.Context context = msgProcessingTimer.time();
        final Timer.Context contextByMsgType = msgProcessingTimerByMsgType.time();

        meteredActor.onReceive(message);

        //stop timers
        contextByMsgType.stop();
        context.stop();
    }
}
