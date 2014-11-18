package org.opendaylight.controller.cluster.raft.notifications;

import akka.actor.Actor;
import akka.actor.Props;
import akka.japi.Creator;
import com.google.common.collect.Sets;
import java.util.Set;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.notifications.ClusterRoleChangeNotification;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;

/**
 * The RaftRoleChangeNotifier is responsible for receiving Raft role change messages and notifying
 * the listeners (within the same node), which are registered with it.
 * <p/>
 * The RaftRoleChangeNotifier is instantiated from the RaftActor.
 */
public class RaftRoleChangeNotifier extends AbstractUntypedActor implements AutoCloseable {

    private String memberId;
    private Set<String> registeredListeners = Sets.newHashSet();
    private ClusterRoleChangeNotification latestRoleChangeNotification = null;

    public RaftRoleChangeNotifier(String memberId) {
        this.memberId = memberId;
    }

    public static Props getProps(final String memberId) {
        return Props.create(new Creator<Actor>() {
            @Override
            public Actor create() throws Exception {
                return new RaftRoleChangeNotifier(memberId);
            }
        });
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        LOG.info("RaftRoleChangeNotifier:{} created and ready for shard:{}",
            getSelf().path().toString(), memberId);
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        if (message instanceof RegisterRoleChangeListener) {
            // register listeners for this member
            registeredListeners.add(getSender().path().toString());

            LOG.info("RaftRoleChangeNotifier for {} , registered listener {}", memberId,
                getSender().path().toString());

            getSender().tell(new RegisterRoleChangeListenerReply(getSelf().path().toString()), getSelf());

            if (latestRoleChangeNotification != null) {
                getContext().actorSelection(getSender().path().toString()).tell(latestRoleChangeNotification, getSelf());
            }


        } else if (message instanceof RaftRoleChanged) {
            // notify registered listeners when this message is received.
            RaftRoleChanged roleChanged = (RaftRoleChanged) message;

            LOG.info("RaftRoleChangeNotifier for {} , received role change from {} to {}", memberId,
                roleChanged.getOldRole().name(), roleChanged.getNewRole().name());


            latestRoleChangeNotification =
                new ClusterRoleChangeNotification(roleChanged.getMemberId(),
                    roleChanged.getOldRole().name(), roleChanged.getNewRole().name());

            for (String listener: registeredListeners) {
                // TODO: do i need to resolve and send, do we even care if the listener might be dead
                getContext().actorSelection(listener).tell(latestRoleChangeNotification, getSelf());
            }
        }
    }

    @Override
    public void close() throws Exception {
        registeredListeners.clear();
    }
}
