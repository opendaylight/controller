/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping.ObjectNameAttributeMappingStrategy;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;

public class ObjectNameAttributeReadingStrategy extends AbstractAttributeReadingStrategy {

    private static final Object PREFIX_SEPARATOR = ":";

    public ObjectNameAttributeReadingStrategy(String nullableDefault) {
        super(nullableDefault);
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) throws DocumentedException {

        XmlElement firstChild = configNodes.get(0);
        Preconditions.checkState(configNodes.size() == 1, "This element should be present only once " + firstChild
                + " but was " + configNodes.size());

        Preconditions.checkNotNull(firstChild, "Element %s should be present", firstChild);
        return AttributeConfigElement.create(getNullableDefault(), resolve(firstChild));
    }

    private ObjectNameAttributeMappingStrategy.MappedDependency resolve(XmlElement firstChild) throws DocumentedException{
        XmlElement typeElement = firstChild.getOnlyChildElementWithSameNamespace(XmlMappingConstants.TYPE_KEY);
        Map.Entry<String, String> prefixNamespace = typeElement.findNamespaceOfTextContent();

        String serviceName = checkPrefixAndExtractServiceName(typeElement, prefixNamespace);

        XmlElement nameElement = firstChild.getOnlyChildElementWithSameNamespace(XmlMappingConstants.NAME_KEY);
        String dependencyName = nameElement.getTextContent();

        return new ObjectNameAttributeMappingStrategy.MappedDependency(prefixNamespace.getValue(), serviceName,
                dependencyName);
    }

    public static String checkPrefixAndExtractServiceName(XmlElement typeElement, Map.Entry<String, String> prefixNamespace) throws DocumentedException {
        String serviceName = typeElement.getTextContent();
        Preconditions.checkNotNull(prefixNamespace.getKey(), "Service %s value cannot be linked to namespace",
                XmlMappingConstants.TYPE_KEY);
        if(prefixNamespace.getKey().isEmpty()) {
            return serviceName;
        } else {
            String prefix = prefixNamespace.getKey() + PREFIX_SEPARATOR;
            Preconditions.checkState(serviceName.startsWith(prefix),
                    "Service %s not correctly prefixed, expected %s, but was %s", XmlMappingConstants.TYPE_KEY, prefix,
                    serviceName);
            serviceName = serviceName.substring(prefix.length());
            return serviceName;
        }
    }

}
