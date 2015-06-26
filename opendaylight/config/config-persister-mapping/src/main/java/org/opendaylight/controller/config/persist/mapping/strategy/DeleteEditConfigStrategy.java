/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.mapping.strategy;

import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.persist.mapping.exception.NetconfConfigHandlingException;
import org.opendaylight.controller.config.persist.mapping.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.persist.mapping.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteEditConfigStrategy extends AbstractEditConfigStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteEditConfigStrategy.class);


    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
                               String module, String instance, ServiceRegistryWrapper services) throws NetconfConfigHandlingException {
        throw new NetconfConfigHandlingException(String.format("Unable to delete %s : %s , ServiceInstance not found", module, instance),
                DocumentedException.ErrorType.application,
                DocumentedException.ErrorTag.operation_failed,
                DocumentedException.ErrorSeverity.error);
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta, ObjectName on, ServiceRegistryWrapper services) throws NetconfConfigHandlingException {
        try {
            ta.destroyModule(on);
            LOG.debug("ServiceInstance {} deleted successfully", on);
        } catch (InstanceNotFoundException e) {
            throw new NetconfConfigHandlingException(
                    String.format("Unable to delete %s because of exception %s" + on, e.getMessage()),
                    DocumentedException.ErrorType.application,
                    DocumentedException.ErrorTag.operation_failed,
                    DocumentedException.ErrorSeverity.error);
        }
    }
}
