/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.IdSequence;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.IdSequenceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishNotificationsTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PublishNotificationsTask.class);
    private static final int SECOND_AS_NANO = 1000000000;

    private final NotificationPublishService notificationPublishService;
    private final String notificationId;
    private final long timeToTake;
    private final long delay;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private long sequenceNumber = 1;
    private long startTime;
    private ScheduledFuture<?> scheduledFuture;

    private Exception lastError = null;

    public PublishNotificationsTask(final NotificationPublishService notificationPublishService,
                                    final String notificationId, final long secondsToTake, final long maxPerSecond) {
        Preconditions.checkNotNull(notificationPublishService);
        Preconditions.checkNotNull(notificationId);
        Preconditions.checkArgument(secondsToTake > 0);
        Preconditions.checkArgument(maxPerSecond > 0);

        this.notificationPublishService = notificationPublishService;
        this.notificationId = notificationId;
        this.timeToTake = secondsToTake * SECOND_AS_NANO;
        this.delay = SECOND_AS_NANO / maxPerSecond;

        LOG.debug("Delay : {}", delay);
    }

    @Override
    public void run() {
        final long current = System.nanoTime();

        final IdSequence notification =
                new IdSequenceBuilder().setId(notificationId).setSequenceNumber(sequenceNumber).build();
        sequenceNumber++;

        try {
            LOG.debug("Publishing notification: {}", notification);
            notificationPublishService.putNotification(notification);
        } catch (final InterruptedException e) {
            LOG.warn("Unexpected exception while publishing notification, : {}", notification, e);
            lastError = e;

            //stop on error
            scheduledFuture.cancel(false);
            executor.shutdown();
            return;
        }

        LOG.debug("current {}, starttime: {}, timetotake: {}, current-start = {}",
                current, startTime, timeToTake, current - startTime);

        if ((current - startTime) > timeToTake) {
            LOG.debug("Sequence number: {}", sequenceNumber);
            scheduledFuture.cancel(false);
            executor.shutdown();
        }
    }

    public void start() {
        startTime = System.nanoTime();
        scheduledFuture = executor.scheduleAtFixedRate(this, 0, delay, TimeUnit.NANOSECONDS);
    }

    public boolean isFinished() {
        return scheduledFuture.isCancelled();
    }

    public long getCurrentNotif() {
        return sequenceNumber;
    }

    public Exception getLastError() {
        return lastError;
    }
}
