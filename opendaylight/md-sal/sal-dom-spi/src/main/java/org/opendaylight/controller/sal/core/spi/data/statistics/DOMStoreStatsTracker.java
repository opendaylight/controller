/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.spi.data.statistics;

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;

/**
 * Interface for a class that tracks statistics for a data store.
 *
 * @author Thomas Pantelis
 */
public interface DOMStoreStatsTracker {

    /**
     * Sets the executor used for DataChangeListener notifications.
     *
     * @param dclExecutor the executor
     */
    void setDataChangeListenerExecutor( @Nonnull ExecutorService dclExecutor );

    /**
     * Sets the executor used internally by the data store.
     *
     * @param dsExecutor the executor
     */
    void setDataStoreExecutor( @Nonnull ExecutorService dsExecutor );

    /**
     * Sets the QueuedNotificationManager use for DataChangeListener notifications,
     *
     * @param manager the manager
     */
    void setNotificationManager( @Nonnull QueuedNotificationManager<?, ?> manager );
}
