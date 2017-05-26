/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.threadpool;

import java.util.concurrent.ExecutorService;

/**
 * Interface representing thread pool.
 */
public interface ThreadPool {

    ExecutorService getExecutor();

    int getMaxThreadCount();
}