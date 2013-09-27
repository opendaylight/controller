/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import javax.management.ObjectName;

import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;

public interface ConfigRegistryClient extends ConfigRegistryMXBean {

    ConfigTransactionClient createTransaction();

    ConfigTransactionClient getConfigTransactionClient(String transactionName);

    ConfigTransactionClient getConfigTransactionClient(ObjectName objectName);

    long getVersion();

    Object invokeMethod(ObjectName on, String name, Object[] params,
            String[] signature);

    Object getAttributeCurrentValue(ObjectName on, String attributeName);

}
