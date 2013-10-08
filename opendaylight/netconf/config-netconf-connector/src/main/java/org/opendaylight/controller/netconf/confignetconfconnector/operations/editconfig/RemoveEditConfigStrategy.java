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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RemoveEditConfigStrategy extends DeleteEditConfigStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RemoveEditConfigStrategy.class);

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            String module, String instance) {
        logger.warn("Unable to delete {}:{}, ServiceInstance not found", module, instance);
    }
}
