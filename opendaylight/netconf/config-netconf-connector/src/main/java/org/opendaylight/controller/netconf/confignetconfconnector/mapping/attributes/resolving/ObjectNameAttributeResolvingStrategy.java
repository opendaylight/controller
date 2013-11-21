/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping.ObjectNameAttributeMappingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services.ServiceInstance;
import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import javax.management.openmbean.SimpleType;

public class ObjectNameAttributeResolvingStrategy extends AbstractAttributeResolvingStrategy<ObjectName, SimpleType<?>> {

    private final Services serviceTracker;
    private static final Logger logger = LoggerFactory.getLogger(ObjectNameAttributeResolvingStrategy.class);

    ObjectNameAttributeResolvingStrategy(Services serviceTracker) {
        super(SimpleType.OBJECTNAME);
        this.serviceTracker = serviceTracker;
    }

    @Override
    public Optional<ObjectName> parseAttribute(String attrName, Object value) {
        if (value == null) {
            return Optional.absent();
        }

        Util.checkType(value, ObjectNameAttributeMappingStrategy.MappedDependency.class);

        ObjectNameAttributeMappingStrategy.MappedDependency mappedDep = (ObjectNameAttributeMappingStrategy.MappedDependency) value;
        String serviceName = mappedDep.getServiceName();
        String refName = mappedDep.getRefName();
        String namespace = mappedDep.getNamespace();
        logger.trace("Getting service instance by service name {} : {} and ref name {}", namespace, serviceName, refName);

        ServiceInstance byRefName = serviceTracker.getByServiceAndRefName(namespace, serviceName, refName);
        ObjectName on = ObjectNameUtil.createReadOnlyModuleON(byRefName.getModuleName(), byRefName.getInstanceName());
        logger.debug("Attribute {} : {} parsed to type {}", attrName, value, getOpenType());
        return Optional.of(on);
    }

}
