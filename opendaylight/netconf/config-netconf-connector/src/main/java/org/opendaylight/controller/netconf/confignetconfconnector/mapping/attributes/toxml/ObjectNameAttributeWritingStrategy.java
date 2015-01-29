/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml;

import com.google.common.base.Optional;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping.ObjectNameAttributeMappingStrategy;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ObjectNameAttributeWritingStrategy implements AttributeWritingStrategy {

    private final Document document;
    private final String key;

    /**
     * @param document
     * @param key
     */
    public ObjectNameAttributeWritingStrategy(Document document, String key) {
        this.document = document;
        this.key = key;
    }

    @Override
    public void writeElement(Element parentElement, String namespace, Object value) {
        NetconfUtil.checkType(value, ObjectNameAttributeMappingStrategy.MappedDependency.class);
        Element innerNode = XmlUtil.createElement(document, key, Optional.of(namespace));

        String moduleName = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getServiceName();
        String refName = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getRefName();
        String namespaceForType = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getNamespace();

        Element typeElement = XmlUtil.createTextElementWithNamespacedContent(document,  XmlNetconfConstants.TYPE_KEY, XmlNetconfConstants.PREFIX,
                namespaceForType, moduleName);

        innerNode.appendChild(typeElement);

        final Element nameElement = XmlUtil.createTextElement(document, XmlNetconfConstants.NAME_KEY, refName, Optional.<String>absent());
        innerNode.appendChild(nameElement);

        parentElement.appendChild(innerNode);
    }

}
