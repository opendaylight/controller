/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RuntimeBeanEntryWritingStrategy extends CompositeAttributeWritingStrategy {

    public RuntimeBeanEntryWritingStrategy(final Document document, final String key,
            final Map<String, AttributeWritingStrategy> innerStrats) {
        super(document, key, innerStrats);
    }

    @Override
    public void writeElement(final Element parentElement, final String namespace, final Object value) {
        Util.checkType(value, Map.class);

        Element innerNode = XmlUtil.createElement(getDocument(), getKey(), Optional.<String>absent());

        Map<?, ?> map = (Map<?, ?>) value;

        for (Entry<?, ?> runtimeBeanInstanceMappingEntry : map.entrySet()) {

            // wrap runtime attributes with number assigned to current runtime
            // bean
            Util.checkType(runtimeBeanInstanceMappingEntry.getValue(), Map.class);
            Map<?, ?> innerMap = (Map<?, ?>) runtimeBeanInstanceMappingEntry.getValue();
            Element runtimeInstanceNode = XmlUtil.createElement(getDocument(), "_"
                    + runtimeBeanInstanceMappingEntry.getKey(), Optional.<String>absent());
            innerNode.appendChild(runtimeInstanceNode);

            for (Entry<?, ?> innerObjectEntry : innerMap.entrySet()) {

                Util.checkType(innerObjectEntry.getKey(), String.class);

                String innerKey = (String) innerObjectEntry.getKey();
                Object innerValue = innerObjectEntry.getValue();

                getInnerStrats().get(innerKey).writeElement(runtimeInstanceNode, namespace, innerValue);
            }
        }
        parentElement.appendChild(innerNode);

    }

}
