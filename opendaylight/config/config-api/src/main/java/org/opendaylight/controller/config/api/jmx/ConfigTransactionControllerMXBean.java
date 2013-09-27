/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.jmx;

import org.opendaylight.controller.config.api.ConfigTransactionController;

/**
 * Those are executed by Jolokia clients on configuration transaction
 * represented by {@link ConfigMBeanServer} instance. Note: Reason for having
 * methods in super interface is that JMX allows only one MXBean to be
 * implemented and implementations can expose additional methods to be exported. <br>
 * Implementation of {@link ConfigTransactionController} is not required to
 * implement this interface, but is required to export all methods of
 * ConfigTransactionController to JMX so that this interface can be used as a
 * proxy.
 */
public interface ConfigTransactionControllerMXBean extends
        ConfigTransactionController {

}
