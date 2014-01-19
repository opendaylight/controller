/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ServiceRegistryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import java.util.Map;

public class MissingInstanceHandlingStrategy extends AbstractEditConfigStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MissingInstanceHandlingStrategy.class);

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            String module, String instance, ServiceRegistryWrapper services) {
        ObjectName on = null;
        try {
            on = ta.createModule(module, instance);
            logger.trace("New instance for {} {} created under name {}", module, instance, on);
        } catch (InstanceAlreadyExistsException e1) {
            throw new IllegalStateException("Unable to create instance for " + module + " : " + instance);
        }
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            ObjectName objectName, ServiceRegistryWrapper services) {
        return;
    }
}
