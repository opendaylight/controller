/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.strategy;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.management.Attribute;
import javax.management.ObjectName;
import org.opendaylight.controller.config.facade.xml.exception.ConfigHandlingException;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.facade.xml.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeEditConfigStrategy extends AbstractEditConfigStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MergeEditConfigStrategy.class);

    public MergeEditConfigStrategy() {

    }

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
                               String module, String instance, ServiceRegistryWrapper services) throws
        ConfigHandlingException {
        throw new ConfigHandlingException(
                String.format("Unable to handle missing instance, no missing instances should appear at this point, missing: %s : %s ",
                        module,
                        instance),
                DocumentedException.ErrorType.application,
                DocumentedException.ErrorTag.operation_failed,
                DocumentedException.ErrorSeverity.error);
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta, ObjectName on, ServiceRegistryWrapper services) throws
        ConfigHandlingException {

        for (Entry<String, AttributeConfigElement> configAttributeEntry : configuration.entrySet()) {
            try {
                AttributeConfigElement ace = configAttributeEntry.getValue();

                if (!ace.getResolvedValue().isPresent()) {
                    LOG.debug("Skipping attribute {} for {}", configAttributeEntry.getKey(), on);
                    continue;
                }

                Object toBeMergedIn = ace.getResolvedValue().get();
                // Get the existing values so we can merge the new values with them.
                Attribute currentAttribute = ta.getAttribute(on, ace.getJmxName());
                Object oldValue = (currentAttribute != null ? currentAttribute.getValue() : null);
                // Merge value with currentValue
                toBeMergedIn = merge(oldValue, toBeMergedIn);
                ta.setAttribute(on, ace.getJmxName(), new Attribute(ace.getJmxName(), toBeMergedIn));
                LOG.debug("Attribute {} set to {} for {}", configAttributeEntry.getKey(), toBeMergedIn, on);
            } catch (Exception e) {
                LOG.error("Error while merging objectnames of {}", on, e);
                throw new ConfigHandlingException(String.format("Unable to set attributes for %s, Error with attribute %s : %s ",
                        on,
                        configAttributeEntry.getKey(),
                        configAttributeEntry.getValue()),
                        DocumentedException.ErrorType.application,
                        DocumentedException.ErrorTag.operation_failed,
                        DocumentedException.ErrorSeverity.error);
            }
        }
    }

    /**
     * Merge value into current value
     * Currently, this is only implemented for arrays of ObjectNames, but that is the
     * most common case for which it is needed.
     */
    protected Object merge(Object oldValue, Object toBeMergedIn) {
        if (oldValue instanceof ObjectName[] && toBeMergedIn instanceof ObjectName[]) {
            toBeMergedIn = mergeObjectNameArrays((ObjectName[]) oldValue, (ObjectName[]) toBeMergedIn);
        }
        return toBeMergedIn;
    }

    /**
     * Merge value into current values
     * This implements for arrays of ObjectNames, but that is the
     * most common case for which it is needed.
     *
     * @param oldValue - the new values to be merged into existing values
     * @param toBeMergedIn - the existing values
     *
     * @return an ObjectName[] consisting the elements of currentValue with an elements from values not already present in currentValue added
     *
     */
    protected ObjectName[] mergeObjectNameArrays(ObjectName[] oldValue, ObjectName[] toBeMergedIn) {
        List<ObjectName> newValueList = new ArrayList<>();
        newValueList.addAll(asList(oldValue));
        /*
         It is guaranteed that old values do not contain transaction name.
         Since toBeMergedIn is filled using service references translated by ServiceRegistryWrapper, it
         is also guaranteed that this list will not contain transaction names.
         Run through the list of values to be merged.  If we don't have them already, add them to the list.
         */
        for (ObjectName objName : toBeMergedIn) {
            if (!newValueList.contains(objName)) {
                newValueList.add(objName);
            }
        }
        return newValueList.toArray(new ObjectName[newValueList.size()]);
    }
}
