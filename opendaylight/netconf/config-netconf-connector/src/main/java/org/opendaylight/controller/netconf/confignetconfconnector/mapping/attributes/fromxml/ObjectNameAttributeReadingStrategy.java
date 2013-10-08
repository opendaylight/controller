/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping.ObjectNameAttributeMappingStrategy;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;

import java.util.List;

public class ObjectNameAttributeReadingStrategy extends AbstractAttributeReadingStrategy<AttributeIfc> {

    public ObjectNameAttributeReadingStrategy(DependencyAttribute attributeIfc) {
        super(attributeIfc);
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) {

        XmlElement firstChild = configNodes.get(0);
        Preconditions.checkState(configNodes.size() == 1, "This element should be present only once " + firstChild
                + " but was " + configNodes.size());

        Preconditions.checkNotNull(firstChild, "Element %s should be present", firstChild);
        return AttributeConfigElement.create(getAttributeIfc(), resolve(firstChild));
    }

    private ObjectNameAttributeMappingStrategy.MappedDependency resolve(XmlElement firstChild) {
        XmlElement typeElement = firstChild.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.TYPE_KEY);
        String serviceName = typeElement.getTextContent();
        XmlElement nameElement = firstChild.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.NAME_KEY);
        String dependencyName = nameElement.getTextContent();

        return new ObjectNameAttributeMappingStrategy.MappedDependency(serviceName, dependencyName);
    }

}
