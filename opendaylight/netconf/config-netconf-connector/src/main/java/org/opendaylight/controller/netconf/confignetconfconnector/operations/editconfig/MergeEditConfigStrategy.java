/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.Attribute;
import javax.management.ObjectName;

import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.exception.NetconfConfigHandlingException;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ServiceRegistryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeEditConfigStrategy extends AbstractEditConfigStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MergeEditConfigStrategy.class);

    public MergeEditConfigStrategy() {

    }

    @Override
    void handleMissingInstance(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta,
            String module, String instance, ServiceRegistryWrapper services) throws NetconfConfigHandlingException {
        throw new NetconfConfigHandlingException(
                String.format("Unable to handle missing instance, no missing instances should appear at this point, missing: %s : %s ",
                        module,
                        instance),
                NetconfDocumentedException.ErrorType.application,
                NetconfDocumentedException.ErrorTag.operation_failed,
                NetconfDocumentedException.ErrorSeverity.error);
    }
    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta, ObjectName on, ServiceRegistryWrapper services) throws NetconfConfigHandlingException {

        for (Entry<String, AttributeConfigElement> configAttributeEntry : configuration.entrySet()) {
            try {
                AttributeConfigElement ace = configAttributeEntry.getValue();

                if (!ace.getResolvedValue().isPresent()) {
                    logger.debug("Skipping attribute {} for {}", configAttributeEntry.getKey(), on);
                    continue;
                }

                Object value = ace.getResolvedValue().get();
                // Get the existing values so we can merge the new values with them.
                Attribute currentAttribute = ta.getAttribute(on,ace.getJmxName());
                Object currentValue = (currentAttribute != null ? currentAttribute.getValue() : null);
                // Merge value with currentValue
                value = merge(value, currentValue);
                ta.setAttribute(on, ace.getJmxName(), new Attribute(ace.getJmxName(), value));
                logger.debug("Attribute {} set to {} for {}", configAttributeEntry.getKey(), value, on);
            } catch (Exception e) {
                throw new NetconfConfigHandlingException(String.format("Unable to set attributes for %s, Error with attribute %s : %s ",
                        on,
                        configAttributeEntry.getKey(),
                        configAttributeEntry.getValue()),
                        NetconfDocumentedException.ErrorType.application,
                        NetconfDocumentedException.ErrorTag.operation_failed,
                        NetconfDocumentedException.ErrorSeverity.error);
            }
        }
    }
    /*
     * Merge value into current value
     * Currently, this is only implemented for arrays of ObjectNames, but that is the
     * most common case for which it is needed.
     */
    protected Object merge(Object value, Object currentValue) {
        if(currentValue != null &&
                currentValue instanceof ObjectName[] &&
                value != null &&
                value instanceof ObjectName[]) {
            value =  mergeObjectNameArrays((ObjectName[])value, (ObjectName[])currentValue);
        }
        return value;
    }
    /*
     * Merge value into currentvalues
     * This implements for arrays of ObjectNames, but that is the
     * most common case for which it is needed.
     *
     * @param value - the new values to be merged into existing values
     * @param currentValue - the existing values
     *
     * @return an ObjectName[] consisting the elements of currentValue with an elements from values not already present in currentValue added
     *
     */
    protected ObjectName[] mergeObjectNameArrays(ObjectName[] value, ObjectName[] currentValue) {
        /*
         *  Check to make sure value has length >0 so we can extract it's transactionName
         *  Check to make sure currentValue has length >0, else this is uninteresting
         */
        if(value != null && value.length > 0 && currentValue != null && currentValue.length > 0) {
            // Get the transaction name, because we will need to add it to the existing ObjectNames
            String transactionName = ObjectNameUtil.getTransactionName(value[0]);
            /*
             *  Lists are just much easier to deal with.  Note that Arrays.asList returns a fixed length list
             *  and so you can't add to it.
             */
            List<ObjectName> valueList = Arrays.asList(value);
            List<ObjectName> newValueList = new ArrayList();
            /*
             *  Add a transactionName to all existing values and write them into newValueList.
             *  Since the values being merged in at this stage all have transactions ids in their
             *  names, we can't do the comparison if we don't do this.
             */
            for(ObjectName objName:currentValue) {
                ObjectName withT = ObjectNameUtil.withTransactionName(objName, transactionName);
                newValueList.add(withT);
            }
            /*
             * Run through the list of values to be merged.  If we don't have them already, add them to the list.
             */
            for(ObjectName objName:valueList) {
                if(!newValueList.contains(objName)) {
                    newValueList.add(objName);
                }
            }
            /*
             * We have to very very very carefully put all of these back in an ObjectName[] array
             * as that is what is checked for in DynamicWritableWrapper.fixObjectNames to strip transactionNames
             * ObjectNames come in as strings like:
             * org.opendaylight.controller:TransactionName=ConfigTransaction-36-37,type=ServiceReference,RefName=binding-data-broker,serviceQName="(urn:opendaylight:params:xml:ns:yang:controller:md:sal:binding\?revision=2013-10-28)binding-async-data-broker"
             *
             * Note the "TransactionName=ConfigTransaction-36-37" at the beginning.
             * That transactionName gets stripped from the ObjectName by DynamicWritableWrapper.fixObjectName(s) as it makes
             * its way to the Module.  As it detects whether to do so with 'instanceof ObjectName[]' its important that
             * we get all of this wrapped in an ObjectName[].  We cannot use List.asArray() because it returns Object[].
             */
            ObjectName[] newValue = new ObjectName[newValueList.size()];
            for(int i=0;i< newValueList.size();i++) {
                newValue[i]=newValueList.get(i);
            }
            value = newValue;
        }
        return value;
    }
}
