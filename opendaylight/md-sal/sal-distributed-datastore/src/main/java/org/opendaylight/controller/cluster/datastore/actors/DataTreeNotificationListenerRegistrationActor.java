/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistrationReply;
import org.opendaylight.yangtools.concepts.Registration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Actor co-located with a shard. It exists only to terminate the registration when
 * asked to do so via {@link CloseDataTreeNotificationListenerRegistration}.
 */
public final class DataTreeNotificationListenerRegistrationActor extends AbstractUntypedActor {
    // FIXME: rework this constant to a duration and its injection
    @VisibleForTesting
    static long killDelay = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

    private SetRegistration registration = null;
    private Cancellable killSchedule = null;
    private boolean closed;

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof CloseDataTreeNotificationListenerRegistration) {
            closeListenerRegistration();
            if (isValidSender(getSender())) {
                getSender().tell(CloseDataTreeNotificationListenerRegistrationReply.getInstance(), getSelf());
            }
        } else if (message instanceof SetRegistration setRegistration) {
            registration = setRegistration;
            if (closed) {
                closeListenerRegistration();
            }
        } else {
            unknownMessage(message);
        }
    }

    private void closeListenerRegistration() {
        closed = true;

        final var reg = registration;
        if (reg != null) {
            registration = null;
            reg.registration.close();
            reg.onClose.run();

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

    @NonNullByDefault
    public record SetRegistration(Registration registration, Runnable onClose) {
        public SetRegistration {
            requireNonNull(registration);
            requireNonNull(onClose);
        }
    }
}
