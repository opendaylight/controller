/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.facade.xml.strategy;

import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.facade.xml.exception.ConfigHandlingException;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.facade.xml.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;

/**
 * Edit strategy that forces re-creation of a module instance even if the config didn't change.
 *
 * @author Thomas Pantelis
 */
public class ReCreateEditConfigStrategy extends AbstractEditConfigStrategy {

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            String module, String instance, ServiceRegistryWrapper services) throws ConfigHandlingException {
        throw new ConfigHandlingException(
                String.format("Unable to recreate %s : %s, Existing module instance not found", module, instance),
                DocumentedException.ErrorType.application,
                DocumentedException.ErrorTag.operation_failed,
                DocumentedException.ErrorSeverity.error);
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            ObjectName objectName, ServiceRegistryWrapper services) throws ConfigHandlingException {
        try {
            ta.reCreateModule(objectName);
        } catch(InstanceNotFoundException e) {
            throw new ConfigHandlingException(String.format("Unable to recreate instance for %s", objectName),
                    DocumentedException.ErrorType.application,
                    DocumentedException.ErrorTag.operation_failed,
                    DocumentedException.ErrorSeverity.error);
        }
    }
}
