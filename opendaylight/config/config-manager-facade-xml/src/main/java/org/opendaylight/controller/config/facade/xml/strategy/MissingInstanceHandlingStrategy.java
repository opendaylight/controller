/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.strategy;

import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.facade.xml.exception.ConfigHandlingException;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.facade.xml.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissingInstanceHandlingStrategy extends AbstractEditConfigStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MissingInstanceHandlingStrategy.class);

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            String module, String instance, ServiceRegistryWrapper services) throws ConfigHandlingException {
        try {
            ObjectName on = ta.createModule(module, instance);
            LOG.trace("New instance for {} {} created under name {}", module, instance, on);
        } catch (InstanceAlreadyExistsException e1) {
            throw new ConfigHandlingException(String.format("Unable to create instance for %s : %s.", module, instance),
                    DocumentedException.ErrorType.application,
                    DocumentedException.ErrorTag.operation_failed,
                    DocumentedException.ErrorSeverity.error);
        }
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            ObjectName objectName, ServiceRegistryWrapper services) {
    }
}
