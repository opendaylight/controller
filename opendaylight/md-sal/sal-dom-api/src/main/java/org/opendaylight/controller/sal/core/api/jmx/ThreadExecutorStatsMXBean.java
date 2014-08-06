/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.api.jmx;

public interface ThreadExecutorStatsMXBean {

    Long getCurrentThreadPoolSize();

    Long getLargestThreadPoolSize();

    Long getMaxThreadPoolSize();

    Long getCurrentQueueSize();

    Long getMaxQueueSize();

    Long getActiveThreadCount();

    Long getCompletedTaskCount();

    Long getTotalTaskCount();
}
