/**
 * Represents simple service that has getters and setters, but no dependencies.
 * Creates fixed thread pool wrapped in {@link org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc}.
 * Supports changing the number of threads on 'live' executor.

 * @author Tomas Olvecky
 *
 * November 2012
 *
 * Copyright (c) 2012 by Cisco Systems, Inc.
 * All rights reserved.
 */
package org.opendaylight.controller.config.manager.testingservices.threadpool;

