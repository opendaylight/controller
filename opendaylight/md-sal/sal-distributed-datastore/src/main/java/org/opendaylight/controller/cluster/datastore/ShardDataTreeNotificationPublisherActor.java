/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Actor used to generate and publish data tree notifications. This is used to offload the potentially
 * expensive notification generation from the Shard actor.
 *
 * @author Thomas Pantelis
 */
public class ShardDataTreeNotificationPublisherActor<T extends ShardDataTreeNotificationPublisher>
        extends AbstractUntypedActor {
    private final T publisher;
    private final Stopwatch timer = Stopwatch.createUnstarted();
    private final String name;
    private final String logContext;

    protected ShardDataTreeNotificationPublisherActor(final T publisher, final String name, final String logContext) {
        this.publisher = publisher;
        this.name = name;
        this.logContext = logContext;
    }

    protected T publisher() {
        return publisher;
    }

    protected String logContext() {
        return logContext;
    }

    @Override
    protected void handleReceive(Object message) {
        if (message instanceof PublishNotifications) {
            PublishNotifications toPublish = (PublishNotifications)message;
            timer.start();

            try {
                publisher.publishChanges(toPublish.candidate);
            } finally {
                long elapsedTime = timer.elapsed(TimeUnit.MILLISECONDS);

                if (elapsedTime >= ShardDataTreeNotificationPublisher.PUBLISH_DELAY_THRESHOLD_IN_MS) {
                    LOG.warn("{}: Generation of change events for {} took longer than expected. Elapsed time: {}",
                            logContext, name, timer);
                } else {
                    LOG.debug("{}: Elapsed time for generation of change events for {}: {}", logContext, name, timer);
                }

                timer.reset();
            }
        }
    }

    static class PublishNotifications {
        private final DataTreeCandidate candidate;

        PublishNotifications(DataTreeCandidate candidate) {
            this.candidate = candidate;
        }
    }
}
