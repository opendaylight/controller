/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml;

import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping.ObjectNameAttributeMappingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
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
        Util.checkType(value, ObjectNameAttributeMappingStrategy.MappedDependency.class);
        Element innerNode = document.createElement(key);
        XmlUtil.addNamespaceAttr(innerNode, namespace);

        String moduleName = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getServiceName();
        String refName = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getRefName();
        String namespaceForType = ((ObjectNameAttributeMappingStrategy.MappedDependency) value).getNamespace();

        Element typeElement = XmlUtil.createPrefixedTextElement(document, XmlNetconfConstants.TYPE_KEY, XmlNetconfConstants.PREFIX,
                moduleName);
        XmlUtil.addPrefixedNamespaceAttr(typeElement, XmlNetconfConstants.PREFIX, namespaceForType);
        innerNode.appendChild(typeElement);

        final Element nameElement = XmlUtil.createTextElement(document, XmlNetconfConstants.NAME_KEY, refName);
        innerNode.appendChild(nameElement);

        parentElement.appendChild(innerNode);
    }

}
