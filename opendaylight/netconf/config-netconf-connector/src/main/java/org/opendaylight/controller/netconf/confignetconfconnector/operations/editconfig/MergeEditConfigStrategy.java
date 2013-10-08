/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import java.util.Map;
import java.util.Map.Entry;

import javax.management.Attribute;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeEditConfigStrategy extends AbstractEditConfigStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MergeEditConfigStrategy.class);

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            String module, String instance) {
        ObjectName on;
        try {
            on = ta.createModule(module, instance);
            logger.info("New instance for {} {} created under name {}", module, instance, on);
            executeStrategy(configuration, ta, on);
        } catch (InstanceAlreadyExistsException e1) {
            throw new IllegalStateException("Unable to create instance for " + module + " : " + instance);
        }
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta, ObjectName on) {
        for (Entry<String, AttributeConfigElement> configAttributeEntry : configuration.entrySet()) {
            try {
                AttributeConfigElement ace = configAttributeEntry.getValue();

                if (!ace.getResolvedValue().isPresent()) {
                    logger.debug("Skipping attribute {} for {}", configAttributeEntry.getKey(), on);
                    continue;
                }

                Object value = ace.getResolvedValue().get();
                ta.setAttribute(on, ace.getJmxName(), new Attribute(ace.getJmxName(), value));
                logger.debug("Attribute {} set to {} for {}", configAttributeEntry.getKey(), value, on);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to set attributes for " + on + ", Error with attribute "
                        + configAttributeEntry.getKey() + ":" + configAttributeEntry.getValue(), e);
            }
        }
    }
}
