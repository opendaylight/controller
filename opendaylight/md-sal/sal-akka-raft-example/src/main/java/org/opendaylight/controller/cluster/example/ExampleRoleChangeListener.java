/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.example.messages.RegisterListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

/**
 * This is a sample implementation of a Role Change Listener which is an actor, which registers itself
 * to the ClusterRoleChangeNotifier.
 *
 * <p>The Role Change listener receives a SetNotifiers message with the notifiers to register itself with.
 *
 * <p>It kicks of a scheduler which sends registration messages to the notifiers, till it gets a
 *  RegisterRoleChangeListenerReply.
 *
 * <p>If all the notifiers have been regsitered with, then it cancels the scheduler.
 * It starts the scheduler again when it receives a new registration
 */
public class ExampleRoleChangeListener extends AbstractUntypedActor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ExampleRoleChangeListener.class);

    // the akka url should be set to the notifiers actor-system and domain.
    private static final String NOTIFIER_AKKA_URL = "pekko://raft-test@127.0.0.1:2550/user/";
    private static final FiniteDuration DURATION = new FiniteDuration(100, TimeUnit.MILLISECONDS);
    private static final FiniteDuration SCHEDULER_DURATION = new FiniteDuration(1, TimeUnit.SECONDS);

    private final Map<String, Boolean> notifierRegistrationStatus = new HashMap<>();

    private Cancellable registrationSchedule = null;

    public ExampleRoleChangeListener(final @NonNull String memberName) {
        super(memberName);
        scheduleRegistrationListener(SCHEDULER_DURATION);
        populateRegistry(memberName);
    }

    public static Props getProps(final String memberName) {
        return Props.create(ExampleRoleChangeListener.class, memberName);
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final ActorRef getSender() {
        return super.getSender();
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof RegisterListener) {
            // called by the scheduler at intervals to register any unregistered notifiers
            sendRegistrationRequests();

        } else if (message instanceof RegisterRoleChangeListenerReply) {
            // called by the Notifier
            handleRegisterRoleChangeListenerReply(getSender().path().toString());

        } else if (message instanceof RoleChangeNotification notification) {
            // called by the Notifier
            LOG.info("{}: Role Change Notification received for member: {}, old role: {}, new role: {}", logName,
                notification.getMemberId(), notification.getOldRole(), notification.getNewRole());

            // the apps dependent on such notifications can be called here
            //TODO: add implementation here

        }
    }

    private void scheduleRegistrationListener(final FiniteDuration interval) {
        LOG.debug("{}: scheduleRegistrationListener called", logName);
        registrationSchedule = getContext().system().scheduler()
            .schedule(interval, interval, self(), new RegisterListener(), getContext().system().dispatcher(), self());

    }

    private void populateRegistry(final String memberName) {
        final var notifier = new StringBuilder()
            .append(NOTIFIER_AKKA_URL)
            .append(memberName).append("/")
            .append(memberName).append("-notifier").toString();

        if (!notifierRegistrationStatus.containsKey(notifier)) {
            notifierRegistrationStatus.put(notifier, false);
        }

        if (!registrationSchedule.isCancelled()) {
            scheduleRegistrationListener(SCHEDULER_DURATION);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private void sendRegistrationRequests() {
        for (var entry : notifierRegistrationStatus.entrySet()) {
            if (!entry.getValue()) {
                try {
                    LOG.debug("{}: registering {} with {}", logName, self().path(), entry.getKey());
                    ActorRef notifier = Await.result(
                        getContext().actorSelection(entry.getKey()).resolveOne(DURATION), DURATION);

                    notifier.tell(new RegisterRoleChangeListener(), self());

                } catch (Exception e) {
                    LOG.error("{}: Unable to send registration request to notifier {}", logName, entry.getKey(), e);
                }
            }
        }
    }

    private void handleRegisterRoleChangeListenerReply(final String senderId) {
        final var prev = notifierRegistrationStatus.putIfAbsent(senderId, Boolean.TRUE);
        if (prev != null) {
            LOG.warn("{}: RegisterRoleChangeListenerReply received from notifier which is not known to Listener:{}",
                logName, senderId);
            return;
        }

        // cancel the schedule when listener is registered with all notifiers
        if (!registrationSchedule.isCancelled()) {
            boolean cancelScheduler = true;
            for (var value : notifierRegistrationStatus.values()) {
                cancelScheduler = cancelScheduler && value;
            }
            if (cancelScheduler) {
                registrationSchedule.cancel();
            }
        }
    }

    @Override
    public void close() {
        registrationSchedule.cancel();
    }
}
