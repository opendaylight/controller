/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.IdSequence;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.IdSequenceBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishNotificationsTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PublishNotificationsTask.class);

    private final NotificationPublishService notificationPublishService;
    private final String notificationId;
    private final long timeToTake;
    private final long delay;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private long sequenceNumber = 1;
    private long startTime;
    private SettableFuture<RpcResult<Void>> completionFuture;

    public PublishNotificationsTask(final NotificationPublishService notificationPublishService,
                                    final String notificationId, final long secondsToTake, final long maxPerSecond) {
        Preconditions.checkNotNull(notificationPublishService);
        Preconditions.checkNotNull(notificationId);
        Preconditions.checkArgument(secondsToTake > 0);
        Preconditions.checkArgument(maxPerSecond > 0);

        this.notificationPublishService = notificationPublishService;
        this.notificationId = notificationId;
        this.timeToTake = secondsToTake * 1000000000;
        this.delay = 1000 / maxPerSecond;
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
            completionFuture.set(RpcResultBuilder.<Void>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
        }

        LOG.debug("current {}, starttime: {}, timetotake: {}, current-start = {}",
                current, startTime, timeToTake, current - startTime);

        if ((current - startTime) < timeToTake) {
            executor.schedule(this, delay, TimeUnit.MILLISECONDS);
        } else {
            completionFuture.set(RpcResultBuilder.<Void>success().build());
        }
    }

    public void start(final SettableFuture<RpcResult<Void>> settableFuture) {
        startTime = System.nanoTime();
        completionFuture = settableFuture;
        executor.schedule(this, 0, TimeUnit.MILLISECONDS);
    }
}
