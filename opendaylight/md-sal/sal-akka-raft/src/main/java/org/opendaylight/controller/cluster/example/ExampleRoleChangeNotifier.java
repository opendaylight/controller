package org.opendaylight.controller.cluster.example;

import akka.actor.Actor;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import com.google.common.collect.Maps;
import java.util.Map;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.example.messages.ExampleRoleChangeNotification;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.raft.base.messages.RaftRoleChanged;

/**
 * Each cluster actor extending RaftActor can create a notifier to notify its listeners with
 * the Role changes.
 */
public class ExampleRoleChangeNotifier  extends AbstractUntypedActor implements AutoCloseable {

    private String memberId;
    private Map<ActorPath, ActorRef> registeredListeners = Maps.newHashMap();
    private ExampleRoleChangeNotification latestRoleChangeNotification = null;

    public ExampleRoleChangeNotifier(String memberId) {
        this.memberId = memberId;
    }

    public static Props getProps(final String memberId) {
        return Props.create(new Creator<Actor>() {
            @Override
            public Actor create() throws Exception {
                return new ExampleRoleChangeNotifier(memberId);
            }
        });
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        LOG.info("ExampleRoleChangeNotifier:{} created and ready for actor:{}",
            getSelf().path().toString(), memberId);
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        if (message instanceof RegisterRoleChangeListener) {
            // register listeners for this shard

            ActorRef curRef = registeredListeners.get(getSender().path());
            if (curRef != null) {
                // ActorPaths would pass equal even if the unique id of the actors are different
                // if a listener actor is re-registering after reincarnation, then removing the existing
                // entry so the actor path with correct unique id is registered.
                registeredListeners.remove(getSender().path());
            }
            registeredListeners.put(getSender().path(), getSender());

            LOG.info("ExampleRoleChangeNotifier for {} , registered listener {}", memberId,
                getSender().path().toString());

            getSender().tell(new RegisterRoleChangeListenerReply(), getSelf());

            if (latestRoleChangeNotification != null) {
                getSender().tell(latestRoleChangeNotification, getSelf());
            }


        } else if (message instanceof RaftRoleChanged) {
            // this message is sent by RaftActor. Notify registered listeners when this message is received.
            RaftRoleChanged roleChanged = (RaftRoleChanged) message;

            LOG.info("ExampleRoleChangeNotifier for {} , received role change from {} to {}",
                memberId,
                roleChanged.getOldRole().name(), roleChanged.getNewRole().name());


            latestRoleChangeNotification =
                new ExampleRoleChangeNotification(roleChanged.getMemberId(),
                    roleChanged.getOldRole().name(), roleChanged.getNewRole().name());

            for (ActorRef listener : registeredListeners.values()) {
                listener.tell(latestRoleChangeNotification, getSelf());
            }
        }
    }

    @Override
    public void close() throws Exception {
        registeredListeners.clear();
    }
}
