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
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor used to generate and publish data tree notifications. This is used to offload the potentially
 * expensive notification generation from the Shard actor.
 *
 * @author Thomas Pantelis
 */
public class ShardDataTreeNotificationPublisherActor<T extends ShardDataTreeNotificationPublisher>
        extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTreeNotificationPublisherActor.class);

    private final Stopwatch timer = Stopwatch.createUnstarted();
    private final T publisher;
    private final String name;

    protected ShardDataTreeNotificationPublisherActor(final String logName, final T publisher, final String name) {
        super(logName);
        this.publisher = publisher;
        this.name = name;
    }

    protected T publisher() {
        return publisher;
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof PublishNotifications toPublish) {
            timer.start();
            try {
                publisher.publishChanges(toPublish.candidate);
            } finally {
                long elapsedTime = timer.elapsed(TimeUnit.MILLISECONDS);

                if (elapsedTime >= ShardDataTreeNotificationPublisher.PUBLISH_DELAY_THRESHOLD_IN_MS) {
                    LOG.warn("{}: Generation of change events for {} took longer than expected. Elapsed time: {}",
                            logName, name, timer);
                } else {
                    LOG.debug("{}: Elapsed time for generation of change events for {}: {}", logName, name, timer);
                }

                timer.reset();
            }
        }
    }

    static class PublishNotifications {
        private final DataTreeCandidate candidate;

        PublishNotifications(final DataTreeCandidate candidate) {
            this.candidate = candidate;
        }
    }
}
