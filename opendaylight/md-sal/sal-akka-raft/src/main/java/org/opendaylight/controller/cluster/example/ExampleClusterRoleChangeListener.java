package org.opendaylight.controller.cluster.example;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.Creator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.example.messages.RegisterListener;
import org.opendaylight.controller.cluster.example.messages.SetNotifiers;
import org.opendaylight.controller.cluster.notifications.ClusterRoleChangeNotification;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

/**
 * This is a sample implementation of a Role Change Listener, which registers itself to the ClusterRoleChangeNotifier
 * <p/>
 * The Role Change listener receives a SetNotifiers message with the notifiers to register itself with.
 * <p/>
 * It kicks of a scheduler which sents registration messages to the notifiers, till it gets a RegisterRoleChangeListenerReply
 * <p/>
 * If all the notifiers have been regsitered with, then it cancels the scheduler.
 * It starts the scheduler again when it receives a new registration
 *
 */
public class ExampleClusterRoleChangeListener extends AbstractUntypedActor implements AutoCloseable{
    // the akka url should be set to the notifiers actor-system and domain.
    private static final String AKKA_URL = "akka://raft-test/user/";

    private Map<String, Boolean> notifierRegistrationStatus = new HashMap<>();
    private Cancellable registrationSchedule = null;
    private static final FiniteDuration duration = new FiniteDuration(100, TimeUnit.MILLISECONDS);
    private static final FiniteDuration schedulerDuration = new FiniteDuration(1, TimeUnit.SECONDS);

    public ExampleClusterRoleChangeListener() {
        super();
        scheduleRegistrationListener(schedulerDuration);
    }

    public static Props getProps() {
        return Props.create(new Creator<Actor>() {
            @Override
            public Actor create() throws Exception {
                return new ExampleClusterRoleChangeListener();
            }
        });
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        if (message instanceof RegisterListener) {
            sendRegistrationRequests();

        } else if (message instanceof RegisterRoleChangeListenerReply) {
            RegisterRoleChangeListenerReply reply = (RegisterRoleChangeListenerReply) message;
            handleRegisterRoleChangeListenerReply(reply);

        } else if (message instanceof SetNotifiers) {
            SetNotifiers setNotifiers = (SetNotifiers) message;
            listenTo(setNotifiers.getNotifierList());

        } else if (message instanceof ClusterRoleChangeNotification) {
            ClusterRoleChangeNotification notification = (ClusterRoleChangeNotification) message;

            LOG.info("Role Change Notification received for member:{}, old role:{}, new role:{}",
                notification.getMemberId(), notification.getOldRole(), notification.getNewRole());

            // the apps dependent on such notifications can be called here

        }
    }

    private void listenTo(List<String> clusterMembers) {
        for (String memberId : clusterMembers) {
            String notifier = AKKA_URL + memberId + "/" + memberId + "-notifier";
            if (!notifierRegistrationStatus.containsKey(notifier)) {
                notifierRegistrationStatus.put(notifier, false);
            }
        }

        if (!registrationSchedule.isCancelled()) {
            scheduleRegistrationListener(schedulerDuration);
        }
    }

    private void scheduleRegistrationListener(FiniteDuration interval) {
        LOG.debug("--->scheduleRegistrationListener called.");
        registrationSchedule = getContext().system().scheduler().schedule(
            interval, interval, getSelf(), new RegisterListener(),
            getContext().system().dispatcher(), getSelf());

    }

    private void sendRegistrationRequests() {
        for (Map.Entry<String, Boolean> entry : notifierRegistrationStatus.entrySet()) {
            if (!entry.getValue()) {
                try {
                    ActorRef notifier = Await.result(
                        getContext().actorSelection(entry.getKey()).resolveOne(duration), duration);

                    notifier.tell(new RegisterRoleChangeListener(), getSelf());

                } catch (Exception e) {
                    LOG.error("ERROR!! Unable to send registration request to notifier {}", entry.getKey());
                }
            }
        }
    }

    private void handleRegisterRoleChangeListenerReply(RegisterRoleChangeListenerReply regReply) {
        if (notifierRegistrationStatus.containsKey(regReply.getSenderId())) {
            notifierRegistrationStatus.put(regReply.getSenderId(), true);

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
                regReply.getSenderId());
        }
    }


    @Override
    public void close() throws Exception {
        registrationSchedule.cancel();
    }
}
