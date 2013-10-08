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

public class ReplaceEditConfigStrategy extends AbstractEditConfigStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ReplaceEditConfigStrategy.class);

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            String module, String instance) {
        try {
            ObjectName on = ta.createModule(module, instance);
            logger.debug("New instance for {} {} created under name {}", module, instance, on);
            executeStrategy(configuration, ta, on);
        } catch (InstanceAlreadyExistsException e) {
            logger.warn("Error creating instance {}:{}, replace failed", module, instance, e);
            throw new IllegalStateException("Unable to create new instance for " + module + " : " + instance, e);
        }
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta, ObjectName on) {
        for (Entry<String, AttributeConfigElement> configAttributeEntry : configuration.entrySet()) {
            try {
                AttributeConfigElement ace = configAttributeEntry.getValue();

                if (!ace.getResolvedValue().isPresent()) {
                    Object value = ace.getResolvedDefaultValue();
                    ta.setAttribute(on, ace.getJmxName(), new Attribute(ace.getJmxName(), value));
                    logger.debug("Attribute {} set to default value {} for {}", configAttributeEntry.getKey(), value,
                            on);
                } else {
                    Object value = ace.getResolvedValue().get();
                    ta.setAttribute(on, ace.getJmxName(), new Attribute(ace.getJmxName(), value));
                    logger.debug("Attribute {} set to value {} for {}", configAttributeEntry.getKey(), value, on);
                }

            } catch (Exception e) {
                throw new IllegalStateException("Unable to set attributes for " + on + ", Error with attribute "
                        + configAttributeEntry.getKey() + ":" + configAttributeEntry.getValue(), e);
            }
        }
    }
}
