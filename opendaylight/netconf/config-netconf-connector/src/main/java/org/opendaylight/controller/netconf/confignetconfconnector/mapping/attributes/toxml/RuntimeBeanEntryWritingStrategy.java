/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RuntimeBeanEntryWritingStrategy extends CompositeAttributeWritingStrategy {

    public RuntimeBeanEntryWritingStrategy(Document document, String key,
            Map<String, AttributeWritingStrategy> innerStrats) {
        super(document, key, innerStrats);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.config.netconf.mapping.attributes.toxml.
     * AttributeWritingStrategy#writeElement(org.w3c.dom.Element,
     * java.lang.Object)
     */
    @Override
    public void writeElement(Element parentElement, String namespace, Object value) {
        Util.checkType(value, Map.class);

        Element innerNode = document.createElement(key);

        Map<?, ?> map = (Map<?, ?>) value;

        for (Entry<?, ?> runtimeBeanInstanceMappingEntry : map.entrySet()) {

            // wrap runtime attributes with number assigned to current runtime
            // bean
            Util.checkType(runtimeBeanInstanceMappingEntry.getValue(), Map.class);
            Map<?, ?> innerMap = (Map<?, ?>) runtimeBeanInstanceMappingEntry.getValue();
            Element runtimeInstanceNode = document.createElement("_"
                    + (String) runtimeBeanInstanceMappingEntry.getKey());
            innerNode.appendChild(runtimeInstanceNode);

            for (Entry<?, ?> innerObjectEntry : innerMap.entrySet()) {

                Util.checkType(innerObjectEntry.getKey(), String.class);

                String innerKey = (String) innerObjectEntry.getKey();
                Object innerValue = innerObjectEntry.getValue();

                innerStrats.get(innerKey).writeElement(runtimeInstanceNode, namespace, innerValue);
            }
        }
        parentElement.appendChild(innerNode);

    }

}
