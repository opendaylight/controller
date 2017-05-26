/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.strategy;

import java.util.Map;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.facade.xml.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveEditConfigStrategy extends DeleteEditConfigStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(RemoveEditConfigStrategy.class);

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
                               String module, String instance, ServiceRegistryWrapper services) {
        LOG.warn("Unable to delete {}:{}, ServiceInstance not found", module, instance);
    }
}
