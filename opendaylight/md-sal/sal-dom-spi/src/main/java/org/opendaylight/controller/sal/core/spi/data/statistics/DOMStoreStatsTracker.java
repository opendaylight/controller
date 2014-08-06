/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.spi.data.statistics;

import java.util.concurrent.ExecutorService;

/**
 * Interface for a class that tracks statistics for a data store.
 *
 * @author Thomas Pantelis
 */
public interface DOMStoreStatsTracker {

    void setDataChangeListenerExecutor( ExecutorService dclExecutor );

    void setDataStoreExecutor( ExecutorService dsExecutor );
}
