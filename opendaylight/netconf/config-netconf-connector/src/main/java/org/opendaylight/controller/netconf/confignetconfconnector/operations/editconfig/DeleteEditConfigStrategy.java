/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.confignetconfconnector.exception.StrategyInvocationException;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ServiceRegistryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteEditConfigStrategy extends AbstractEditConfigStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DeleteEditConfigStrategy.class);

    private final Multimap<String, String> providedServices;

    public DeleteEditConfigStrategy() {
        this.providedServices = HashMultimap.create();
    }

    public DeleteEditConfigStrategy(Multimap<String, String> providedServices) {
        this.providedServices = providedServices;
    }

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
                               String module, String instance, ServiceRegistryWrapper services) throws StrategyInvocationException {
        throw new StrategyInvocationException("Unable to delete " + module + ":" + instance + " , ServiceInstance not found");
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta, ObjectName on, ServiceRegistryWrapper services) throws StrategyInvocationException {
        try {
            ta.destroyModule(on);
            logger.debug("ServiceInstance {} deleted successfully", on);
        } catch (InstanceNotFoundException e) {
            throw new StrategyInvocationException(String.format("Unable to delete %s because of exception %s" + on, e.getMessage()));
        }
    }
}
