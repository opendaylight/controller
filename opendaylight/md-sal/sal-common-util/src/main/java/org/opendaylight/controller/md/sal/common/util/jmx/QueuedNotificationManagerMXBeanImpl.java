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
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;

import com.google.common.base.Preconditions;

/**
 * Implementation of the QueuedNotificationManagerMXBean interface.
 *
 * <p>
 * This class is not intended for use outside of MD-SAL and its part of private
 * implementation (still exported as public to be reused across MD-SAL implementation
 * components) and may be removed in subsequent
 * releases.
 *
 * @author Thomas Pantelis
 */
public class QueuedNotificationManagerMXBeanImpl extends AbstractMXBean
                                                 implements QueuedNotificationManagerMXBean {

    private final QueuedNotificationManager<?,?> manager;

    public QueuedNotificationManagerMXBeanImpl( QueuedNotificationManager<?,?> manager,
            String mBeanName, String mBeanType, String mBeanCategory ) {
        super(mBeanName, mBeanType, mBeanCategory);
        this.manager = Preconditions.checkNotNull( manager );
    }

    @Override
    public List<ListenerNotificationQueueStats> getCurrentListenerQueueStats() {
        return manager.getListenerNotificationQueueStats();
    }

    @Override
    public int getMaxListenerQueueSize() {
        return manager.getMaxQueueCapacity();
    }

    public QueuedNotificationManagerStats toQueuedNotificationManagerStats() {
        return new QueuedNotificationManagerStats( getMaxListenerQueueSize(),
                getCurrentListenerQueueStats() );
    }
}
