/*
 * Copyright (c) 2019 Lumina Networks, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.actor.AbstractActor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.pattern.Backoff;
import akka.pattern.BackoffOptions;
import akka.pattern.BackoffSupervisor;
import com.google.common.base.Preconditions;
import scala.concurrent.duration.FiniteDuration;

/**
 * Utilities for creating back-off supervisor actor. When the child actor stops,
 * this actor will restart it using back-off supervision strategy.
 */
public final class BackoffSupervisorUtils {

    private static final String CHILD_ACTOR_SUFFIX = "-backoff-supervised";
    private static final double BACKOFF_RANDOM_FACTOR = 0.2;

    private BackoffSupervisorUtils() {}

    /**
     * Create back-off supervisor actor which creates and supervises child actor.
     *
     * @param actorSystem the actor system under which back-off supervisor actor
     *        will be created
     * @param minBackoff the minimum back-off duration
     * @param maxBackOff the maximum back-off duration
     * @param resetBackoff the back-off reset duration
     * @param actorName the name of back-off supervisor actor
     * @param supervisionStrategy the supervision strategy
     * @param childActorName the name of child actor
     * @param childActorProps the child actor Props
     * @return the ActorRef for the created back-off supervisor actor
     */
    public static ActorRef createBackoffSupervisor(final ActorSystem actorSystem, final FiniteDuration minBackoff,
            final FiniteDuration maxBackOff, final FiniteDuration resetBackoff, final String actorName,
            final OneForOneStrategy supervisionStrategy, final String childActorName, final Props childActorProps) {
        Preconditions.checkArgument((actorName != null) || (childActorName != null));

        final Props supervisorProps =
                getBackoffSupervisorProps(minBackoff, maxBackOff, resetBackoff, supervisionStrategy,
                        childActorName != null ? childActorName : getChildActorName(actorName), childActorProps);

        if (actorName != null) {
            return actorSystem.actorOf(supervisorProps, actorName);
        } else {
            return actorSystem.actorOf(supervisorProps);
        }
    }

    /**
     * Create back-off supervisor actor which creates and supervises child actor.
     *
     * @param actorContext the actor context under which back-off supervisor actor
     *        will be created
     * @param minBackoff the minimum back-off duration
     * @param maxBackOff the maximum back-off duration
     * @param resetBackoff the back-off reset duration
     * @param actorName the name of back-off supervisor actor
     * @param supervisionStrategy the supervision strategy
     * @param childActorName the name of child actor
     * @param childActorProps the child actor Props
     * @return the ActorRef for the created back-off supervisor actor
     */
    public static ActorRef createBackoffSupervisor(final ActorContext actorContext,
            final FiniteDuration minBackoff, final FiniteDuration maxBackOff, final FiniteDuration resetBackoff,
            final String actorName, final OneForOneStrategy supervisionStrategy, final String childActorName,
            final Props childActorProps) {
        Preconditions.checkArgument((actorName != null) || (childActorName != null));

        final Props supervisorProps =
                getBackoffSupervisorProps(minBackoff, maxBackOff, resetBackoff, supervisionStrategy,
                        childActorName != null ? childActorName : getChildActorName(actorName), childActorProps);

        if (actorName != null) {
            return actorContext.actorOf(supervisorProps, actorName);
        } else {
            return actorContext.actorOf(supervisorProps);
        }
    }

    private static Props getBackoffSupervisorProps(final FiniteDuration minBackoff, final FiniteDuration maxBackOff,
            final FiniteDuration resetBackoff, final OneForOneStrategy supervisionStrategy, final String childActorName,
            final Props childActorProps) {
        final BackoffOptions backoffOptions =
                Backoff.onStop(childActorProps, childActorName, minBackoff, maxBackOff, BACKOFF_RANDOM_FACTOR)
                        .withAutoReset(resetBackoff);
        if (supervisionStrategy != null) {
            return BackoffSupervisor.props(backoffOptions.withSupervisorStrategy(supervisionStrategy));
        } else {
            return BackoffSupervisor.props(backoffOptions);
        }
    }

    /**
     * Create child actor name to be used while creating back-off supervisor actor.
     *
     * @param actorName the back-off supervisor actor name
     * @return the child actor name
     */
    public static String getChildActorName(final String actorName) {
        return actorName + CHILD_ACTOR_SUFFIX;
    }
}
