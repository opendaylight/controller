/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.seviceinterface;

import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc;

@ServiceInterfaceAnnotation(value = TestingThreadPoolServiceInterface.QNAME, osgiRegistrationType = TestingThreadPoolIfc.class,
    namespace = "ns", revision = "foo", localName = "testing-threadpool")
public interface TestingThreadPoolServiceInterface extends
        AbstractServiceInterface {
    String QNAME = "(ns?revision=foo)testing-threadpool";
}
