/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.example.messages.RegisterListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

/**
 * This is a sample implementation of a Role Change Listener which is an actor, which registers itself to the ClusterRoleChangeNotifier
 * <p/>
 * The Role Change listener receives a SetNotifiers message with the notifiers to register itself with.
 * <p/>
 * It kicks of a scheduler which sents registration messages to the notifiers, till it gets a RegisterRoleChangeListenerReply
 * <p/>
 * If all the notifiers have been regsitered with, then it cancels the scheduler.
 * It starts the scheduler again when it receives a new registration
 *
 */
public class ExampleRoleChangeListener extends AbstractUntypedActor implements AutoCloseable{
    // the akka url should be set to the notifiers actor-system and domain.
    private static final String NOTIFIER_AKKA_URL = "akka.tcp://raft-test@127.0.0.1:2550/user/";

    private Map<String, Boolean> notifierRegistrationStatus = new HashMap<>();
    private Cancellable registrationSchedule = null;
    private static final FiniteDuration duration = new FiniteDuration(100, TimeUnit.MILLISECONDS);
    private static final FiniteDuration schedulerDuration = new FiniteDuration(1, TimeUnit.SECONDS);
    private final String memberName;
    private static final String[] shardsToMonitor = new String[] {"example"};

    public ExampleRoleChangeListener(String memberName) {
        super();
        scheduleRegistrationListener(schedulerDuration);
        this.memberName = memberName;
        populateRegistry(memberName);
    }

    public static Props getProps(final String memberName) {
        return Props.create(ExampleRoleChangeListener.class, memberName);
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        if (message instanceof RegisterListener) {
            // called by the scheduler at intervals to register any unregistered notifiers
            sendRegistrationRequests();

        } else if (message instanceof RegisterRoleChangeListenerReply) {
            // called by the Notifier
            handleRegisterRoleChangeListenerReply(getSender().path().toString());

        } else if (message instanceof RoleChangeNotification) {
            // called by the Notifier
            RoleChangeNotification notification = (RoleChangeNotification) message;

            LOG.info("Role Change Notification received for member:{}, old role:{}, new role:{}",
                notification.getMemberId(), notification.getOldRole(), notification.getNewRole());

            // the apps dependent on such notifications can be called here
            //TODO: add implementation here

        }
    }

    private void scheduleRegistrationListener(FiniteDuration interval) {
        LOG.debug("--->scheduleRegistrationListener called.");
        registrationSchedule = getContext().system().scheduler().schedule(
            interval, interval, getSelf(), new RegisterListener(),
            getContext().system().dispatcher(), getSelf());

    }

    private void populateRegistry(String memberName) {

        for (String shard: shardsToMonitor) {
            String notifier =(new StringBuilder()).append(NOTIFIER_AKKA_URL).append(memberName)
                .append("/").append(memberName).append("-notifier").toString();

            if (!notifierRegistrationStatus.containsKey(notifier)) {
                notifierRegistrationStatus.put(notifier, false);
            }
        }

        if (!registrationSchedule.isCancelled()) {
            scheduleRegistrationListener(schedulerDuration);
        }
    }

    private void sendRegistrationRequests() {
        for (Map.Entry<String, Boolean> entry : notifierRegistrationStatus.entrySet()) {
            if (!entry.getValue()) {
                try {
                    LOG.debug("{} registering with {}", getSelf().path().toString(), entry.getKey());
                    ActorRef notifier = Await.result(
                        getContext().actorSelection(entry.getKey()).resolveOne(duration), duration);

                    notifier.tell(new RegisterRoleChangeListener(), getSelf());

                } catch (Exception e) {
                    LOG.error("ERROR!! Unable to send registration request to notifier {}", entry.getKey());
                }
            }
        }
    }

    private void handleRegisterRoleChangeListenerReply(String senderId) {
        if (notifierRegistrationStatus.containsKey(senderId)) {
            notifierRegistrationStatus.put(senderId, true);

            //cancel the schedule when listener is registered with all notifiers
            if (!registrationSchedule.isCancelled()) {
                boolean cancelScheduler = true;
                for (Boolean value : notifierRegistrationStatus.values()) {
                    cancelScheduler = cancelScheduler & value;
                }
                if (cancelScheduler) {
                    registrationSchedule.cancel();
                }
            }
        } else {
            LOG.info("Unexpected, RegisterRoleChangeListenerReply received from notifier which is not known to Listener:{}",
                senderId);
        }
    }


    @Override
    public void close() throws Exception {
        registrationSchedule.cancel();
    }
}
