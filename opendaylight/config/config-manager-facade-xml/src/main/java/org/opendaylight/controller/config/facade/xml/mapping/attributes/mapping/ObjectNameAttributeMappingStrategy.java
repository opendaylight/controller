/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.management.ObjectName;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.yangtools.yang.common.QName;

public class ObjectNameAttributeMappingStrategy extends
        AbstractAttributeMappingStrategy<ObjectNameAttributeMappingStrategy.MappedDependency, SimpleType<?>> {

    private final String namespace;

    public ObjectNameAttributeMappingStrategy(final SimpleType<?> openType, final String namespace) {
        super(openType);
        this.namespace = namespace;
    }

    @Override
    public Optional<MappedDependency> mapAttribute(final Object value) {
        if (value == null){
            return Optional.absent();
        }

        String expectedClass = getOpenType().getClassName();
        String realClass = value.getClass().getName();
        Preconditions.checkArgument(realClass.equals(expectedClass), "Type mismatch, expected " + expectedClass
                + " but was " + realClass);
        Util.checkType(value, ObjectName.class);

        ObjectName on = (ObjectName) value;

        String refName = ObjectNameUtil.getReferenceName(on);

        //we want to use the exact service name that was configured in xml so services that are referencing it can be resolved
        return Optional.of(new MappedDependency(namespace,
                QName.create(ObjectNameUtil.getServiceQName(on)).getLocalName(), refName));
    }

    public static class MappedDependency {
        private final String namespace, serviceName, refName;

        public MappedDependency(final String namespace, final String serviceName, final String refName) {
            this.serviceName = serviceName;
            this.refName = refName;
            this.namespace = namespace;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getRefName() {
            return refName;
        }

        public String getNamespace() {
            return namespace;
        }

        @Override
        public String toString() {
            return "MappedDependency{"
                    + "namespace='" + namespace + '\''
                    + ", serviceName='" + serviceName + '\''
                    + ", refName='" + refName + '\''
                    + '}';
        }
    }

}
