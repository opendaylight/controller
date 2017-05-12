/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping.ObjectNameAttributeMappingStrategy;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ObjectNameAttributeWritingStrategy implements AttributeWritingStrategy {

    private final Document document;
    private final String key;

    /**
     * @param document
     * @param key
     */
    public ObjectNameAttributeWritingStrategy(final Document document, final String key) {
        this.document = document;
        this.key = key;
    }

    @Override
    public void writeElement(final Element parentElement, final String namespace, final Object value) {
        Util.checkType(value, ObjectNameAttributeMappingStrategy.MappedDependency.class);
        Element innerNode = XmlUtil.createElement(document, key, Optional.of(namespace));

        String moduleName = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getServiceName();
        String refName = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getRefName();
        String namespaceForType = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getNamespace();

        Element typeElement = XmlUtil.createTextElementWithNamespacedContent(document,  XmlMappingConstants.TYPE_KEY, XmlMappingConstants.PREFIX,
                namespaceForType, moduleName);

        innerNode.appendChild(typeElement);

        final Element nameElement = XmlUtil.createTextElement(document, XmlMappingConstants.NAME_KEY, refName, Optional.<String>absent());
        innerNode.appendChild(nameElement);

        parentElement.appendChild(innerNode);
    }

}
