/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving;

import com.google.common.base.Optional;
import javax.management.ObjectName;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping.ObjectNameAttributeMappingStrategy;
import org.opendaylight.controller.config.facade.xml.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectNameAttributeResolvingStrategy extends AbstractAttributeResolvingStrategy<ObjectName, SimpleType<?>> {

    private final ServiceRegistryWrapper serviceTracker;
    private static final Logger LOG = LoggerFactory.getLogger(ObjectNameAttributeResolvingStrategy.class);

    ObjectNameAttributeResolvingStrategy(final ServiceRegistryWrapper serviceTracker) {
        super(SimpleType.OBJECTNAME);
        this.serviceTracker = serviceTracker;
    }

    @Override
    public Optional<ObjectName> parseAttribute(final String attrName, final Object value) {
        if (value == null) {
            return Optional.absent();
        }

        Util.checkType(value, ObjectNameAttributeMappingStrategy.MappedDependency.class);

        ObjectNameAttributeMappingStrategy.MappedDependency mappedDep = (ObjectNameAttributeMappingStrategy.MappedDependency) value;
        String serviceName = mappedDep.getServiceName();
        String refName = mappedDep.getRefName();
        String namespace = mappedDep.getNamespace();
        LOG.trace("Getting service instance by service name {} : {} and ref name {}", namespace, serviceName, refName);

        ObjectName on = serviceTracker.getByServiceAndRefName(namespace, serviceName, refName);

        LOG.debug("Attribute {} : {} parsed to type {}", attrName, value, getOpenType());
        return Optional.of(on);
    }

}
