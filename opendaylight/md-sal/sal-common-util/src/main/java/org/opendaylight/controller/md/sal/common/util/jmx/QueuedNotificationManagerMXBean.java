/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.util.jmx;

import java.util.List;

import org.opendaylight.yangtools.util.concurrent.ListenerNotificationQueueStats;

/**
 * MXBean interface for {@link QueuedNotificationManager} statistic metrics.
 *
 * @author Thomas Pantelis
 */
public interface QueuedNotificationManagerMXBean {

    /**
     * Returns a list of stat instances for each current listener notification task in progress.
     */
    List<ListenerNotificationQueueStats> getCurrentListenerQueueStats();

    /**
     * Returns the configured maximum listener queue size.
     */
    int getMaxListenerQueueSize();
}
