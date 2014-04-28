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
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.exception.NetconfConfigHandlingException;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ServiceRegistryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.util.Map;
import java.util.Map.Entry;

public class MergeEditConfigStrategy extends AbstractEditConfigStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MergeEditConfigStrategy.class);
    private final Multimap<String, String> providedServices;

    public MergeEditConfigStrategy() {
        this.providedServices = HashMultimap.create();
    }

    public MergeEditConfigStrategy(Multimap<String, String> providedServices) {
        this.providedServices = providedServices;
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

    private void addRefNames(ServiceRegistryWrapper services, Multimap<String, String> providedServices, ConfigTransactionClient ta, ObjectName on) throws InstanceNotFoundException {
        for (Entry<String, String> namespaceToService : providedServices.entries()) {

            if(services.hasRefName(namespaceToService.getKey(),
                    namespaceToService.getValue(), on)){
                continue;
            }

            String refName = services.getNewDefaultRefName(namespaceToService.getKey(), namespaceToService.getValue(),
                    ObjectNameUtil.getFactoryName(on), ObjectNameUtil.getInstanceName(on));
            ta.saveServiceReference(
                    ta.getServiceInterfaceName(namespaceToService.getKey(), namespaceToService.getValue()), refName, on);
        }
    }

    @Override
    void executeStrategy(Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta, ObjectName on, ServiceRegistryWrapper services) throws NetconfConfigHandlingException {
        try {
            addRefNames(services, providedServices, ta, on);
        } catch (InstanceNotFoundException e) {
            throw new NetconfConfigHandlingException(String.format("Unable to save default ref name for instance %s. Instance was not found.",e),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error);
        }

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
}
