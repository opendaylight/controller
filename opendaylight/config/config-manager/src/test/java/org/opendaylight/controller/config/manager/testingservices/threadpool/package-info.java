/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Represents simple service that has getters and setters, but no dependencies.
 * Creates fixed thread pool wrapped in {@link org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc}.
 * Supports changing the number of threads on 'live' executor.
 */
package org.opendaylight.controller.config.manager.testingservices.threadpool;
