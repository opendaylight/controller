/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Actor used to generate and publish data tree notifications. This is used to offload the potentially
 * expensive notification generation from the Shard actor.
 *
 * @author Thomas Pantelis
 */
public class ShardDataTreeNotificationPublisherActor extends AbstractUntypedActor {

    @Override
    protected void handleReceive(Object message) {
        if(message instanceof PublishNotifications) {
            ((PublishNotifications)message).publish();
        }
    }

    static Props props() {
        return Props.create(ShardDataTreeNotificationPublisherActor.class);
    }

    static class PublishNotifications {
        private final ShardDataTreeNotificationPublisher publisher;
        private final DataTreeCandidate candidate;
        private final String logContext;

        PublishNotifications(ShardDataTreeNotificationPublisher publisher, DataTreeCandidate candidate,
                String logContext) {
            this.publisher = publisher;
            this.candidate = candidate;
            this.logContext = logContext;
        }

        private void publish() {
            publisher.publishChanges(candidate, logContext);
        }
    }
}
