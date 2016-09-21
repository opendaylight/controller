/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool;

import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolConfigMXBean;

public interface TestingScheduledThreadPoolConfigBeanMXBean extends
        TestingThreadPoolConfigMXBean {

    boolean isRecreate();

    void setRecreate(boolean recreate);

    void setThreadCount(int threadCount);

}
