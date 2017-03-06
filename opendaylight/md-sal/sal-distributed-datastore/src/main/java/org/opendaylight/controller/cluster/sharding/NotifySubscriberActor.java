/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.dom.api.LeaderLocation;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListener;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;

public class NotifySubscriberActor extends AbstractUntypedActor {
    private final LeaderLocationListener leaderLocationListener;
    private final ActorRef roleChangeNotifier;

    private NotifySubscriberActor(final ActorRef roleChangeNotifier, final LeaderLocationListener listener) {
        this.roleChangeNotifier = Preconditions.checkNotNull(roleChangeNotifier);
        this.leaderLocationListener = Preconditions.checkNotNull(listener);
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        roleChangeNotifier.tell(new RegisterRoleChangeListener(), getSelf());
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if (message instanceof RoleChangeNotification) {
            ignoreMessage(message);
        } else if (message instanceof LeaderStateChanged) {
            onLeaderStateChanged((LeaderStateChanged) message);
        } else {
            unknownMessage(message);
        }
    }

    private void onLeaderStateChanged(final LeaderStateChanged message) {
        final LeaderLocation newLocation;
        if (message.getLeaderId() == null) {
            newLocation = LeaderLocation.UNKNOWN;
        } else if (message.getMemberId().equals(message.getLeaderId())) {
            newLocation = LeaderLocation.LOCAL;
        } else {
            newLocation = LeaderLocation.REMOTE;
        }

        // TODO should we wrap this in try catch block?
        leaderLocationListener.onLeaderLocationChanged(newLocation);
    }

    public static Props props(final ActorRef roleChangeNotifier, final LeaderLocationListener listener) {
        return Props.create(new NotifySubscriberCreator(roleChangeNotifier, listener));
    }

    private static final class NotifySubscriberCreator implements Creator<NotifySubscriberActor> {
        private static final long serialVersionUID = 1L;

        private final ActorRef roleChangeNotifier;

        @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but we don't "
                + "create remote instances of this actor and thus don't need it to be Serializable.")
        private final LeaderLocationListener listener;

        NotifySubscriberCreator(final ActorRef roleChangeNotifier, final LeaderLocationListener listener) {
            this.roleChangeNotifier = roleChangeNotifier;
            this.listener = listener;
        }

        @Override
        public NotifySubscriberActor create() throws Exception {
            return new NotifySubscriberActor(roleChangeNotifier, listener);
        }
    }
}
