/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.seviceinterface;

import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.TestingScheduledThreadPoolIfc;

@ServiceInterfaceAnnotation(value = "threadpool-scheduled", osgiRegistrationType = TestingScheduledThreadPoolIfc.class,
    namespace = "ns", revision = "rev", localName = "threadpool-scheduled")
public interface TestingScheduledThreadPoolServiceInterface extends
        TestingThreadPoolServiceInterface {
}
