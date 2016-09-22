/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.parallelapsp;

import javax.management.ObjectName;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;

@ServiceInterfaceAnnotation(value = TestingParallelAPSPConfigMXBean.NAME, osgiRegistrationType = TestingAPSP.class,
    namespace = "namespace", revision = "rev", localName = TestingParallelAPSPConfigMXBean.NAME)
public interface TestingParallelAPSPConfigMXBean {

    String NAME = "apsp";

    ObjectName getThreadPool();

    void setThreadPool(ObjectName threadPoolName);

    String getSomeParam();

    void setSomeParam(String s);

    // for reporting. this should be moved to runtime jmx bean
    Integer getMaxNumberOfThreads();

}
