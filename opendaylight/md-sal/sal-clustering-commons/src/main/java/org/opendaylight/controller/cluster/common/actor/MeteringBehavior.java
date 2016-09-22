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
import com.google.common.base.Throwables;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;

/**
 * Represents behaviour that can be exhibited by actors of type {@link akka.actor.UntypedActor}
 * <p/>
 * This behaviour meters actor's default behaviour. It captures 2 metrics:
 * <ul>
 *     <li>message processing rate of actor's receive block</li>
 *     <li>message processing rate by message type</li>
 * </ul>
 *
 * The information is reported to {@link org.opendaylight.controller.cluster.reporting.MetricsReporter}
 */
public class MeteringBehavior implements Procedure<Object> {
    public static final String DOMAIN = "org.opendaylight.controller.actor.metric";

    private final UntypedActor meteredActor;

    private final MetricRegistry METRICREGISTRY = MetricsReporter.getInstance(DOMAIN).getMetricsRegistry();
    private final String MSG_PROCESSING_RATE = "msg-rate";

    private String actorQualifiedName;
    private Timer msgProcessingTimer;

    /**
     *
     * @param actor whose behaviour needs to be metered
     */
    public MeteringBehavior(AbstractUntypedActorWithMetering actor){
        Preconditions.checkArgument(actor != null, "actor must not be null");
        this.meteredActor = actor;

        String actorName = actor.getActorNameOverride() != null ? actor.getActorNameOverride()
                                                                : actor.getSelf().path().name();
        init(actorName);
    }

    public MeteringBehavior(UntypedActor actor){
        Preconditions.checkArgument(actor != null, "actor must not be null");
        this.meteredActor = actor;

        String actorName = actor.getSelf().path().name();
        init(actorName);
    }

    private void init(String actorName){
        actorQualifiedName = new StringBuilder(meteredActor.getSelf().path().parent().toStringWithoutAddress()).
                append("/").append(actorName).toString();

        final String msgProcessingTime = MetricRegistry.name(actorQualifiedName, MSG_PROCESSING_RATE);
        msgProcessingTimer = METRICREGISTRY.timer(msgProcessingTime);
    }

    /**
     * Uses 2 timers to measure message processing rate. One for overall message processing rate and
     * another to measure rate by message type. The timers are re-used if they were previously created.
     * <p/>
     * {@link com.codahale.metrics.MetricRegistry} maintains a reservoir for different timers where
     * collected timings are kept. It exposes various metrics for each timer based on collected
     * data. Eg: count of messages, 99, 95, 50... percentiles, max, mean etc.
     * <p/>
     * These metrics are exposed as JMX bean.
     *
     * @see <a href="http://dropwizard.github.io/metrics/manual/core/#timers">
     *     http://dropwizard.github.io/metrics/manual/core/#timers</a>
     *
     * @param message
     */
    @Override
    public void apply(Object message) {
        final String messageType = message.getClass().getSimpleName();

        final String msgProcessingTimeByMsgType =
                MetricRegistry.name(actorQualifiedName, MSG_PROCESSING_RATE, messageType);

        final Timer msgProcessingTimerByMsgType = METRICREGISTRY.timer(msgProcessingTimeByMsgType);

        //start timers
        final Timer.Context context = msgProcessingTimer.time();
        final Timer.Context contextByMsgType = msgProcessingTimerByMsgType.time();

        try {
            meteredActor.onReceive(message);
        } catch (Throwable throwable) {
            Throwables.propagate(throwable);
        }

        //stop timers
        contextByMsgType.stop();
        context.stop();
    }
}
