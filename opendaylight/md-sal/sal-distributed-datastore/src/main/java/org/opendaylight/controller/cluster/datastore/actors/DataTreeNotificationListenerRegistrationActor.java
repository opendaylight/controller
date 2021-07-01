/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistrationReply;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Actor co-located with a shard. It exists only to terminate the registration when
 * asked to do so via {@link CloseDataTreeNotificationListenerRegistration}.
 */
public final class DataTreeNotificationListenerRegistrationActor extends AbstractUntypedActor {
    // FIXME: rework this constant to a duration and its injection
    @VisibleForTesting
    static long killDelay = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

    private ListenerRegistration<?> registration;
    private Runnable onClose;
    private boolean closed;
    private Cancellable killSchedule;

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof CloseDataTreeNotificationListenerRegistration) {
            closeListenerRegistration();
            if (isValidSender(getSender())) {
                getSender().tell(CloseDataTreeNotificationListenerRegistrationReply.getInstance(), getSelf());
            }
        } else if (message instanceof SetRegistration) {
            registration = ((SetRegistration)message).registration;
            onClose = ((SetRegistration)message).onClose;
            if (closed) {
                closeListenerRegistration();
            }
        } else {
            unknownMessage(message);
        }
    }

    private void closeListenerRegistration() {
        closed = true;
        if (registration != null) {
            registration.close();
            onClose.run();
            registration = null;

            if (killSchedule == null) {
                killSchedule = getContext().system().scheduler().scheduleOnce(FiniteDuration.create(killDelay,
                        TimeUnit.MILLISECONDS), getSelf(), PoisonPill.getInstance(), getContext().dispatcher(),
                        ActorRef.noSender());
            }
        }
    }

    public static Props props() {
        return Props.create(DataTreeNotificationListenerRegistrationActor.class);
    }

    public static class SetRegistration {
        private final ListenerRegistration<?> registration;
        private final Runnable onClose;

        public SetRegistration(final ListenerRegistration<?> registration, final Runnable onClose) {
            this.registration = requireNonNull(registration);
            this.onClose = requireNonNull(onClose);
        }
    }
}
