package org.opendaylight.controller.cluster.example;

import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.notifications.ClusterRoleChangeNotification;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;

/**
 * Created by kramesha on 11/17/14.
 */
public class ExampleRoleChangeListener extends AbstractUntypedActor {

    @Override
    protected void handleReceive(Object message) throws Exception {
        if (message instanceof RegisterRoleChangeListenerReply) {

        } else if (message instanceof ClusterRoleChangeNotification) {

        }
    }
}
