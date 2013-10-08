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
import org.opendaylight.controller.netconf.util.xml.XmlElement;

import javax.management.openmbean.OpenType;
import java.util.List;

public class SimpleAttributeReadingStrategy extends AbstractAttributeReadingStrategy<AttributeIfc> {

    public SimpleAttributeReadingStrategy(AttributeIfc attributeIfc) {
        super(attributeIfc);
    }

    /**
     * @param elementOpenType
     */
    public SimpleAttributeReadingStrategy(OpenType<?> elementOpenType) {
        super(new AttributeIfcWrapper(elementOpenType));
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) {
        XmlElement xmlElement = configNodes.get(0);
        Preconditions.checkState(configNodes.size() == 1, "This element should be present only once " + xmlElement
                + " but was " + configNodes.size());

        String textContent = xmlElement.getTextContent();

        Preconditions.checkNotNull(textContent, "This element should contain text %s", xmlElement);
        return AttributeConfigElement.create(getAttributeIfc(), textContent);
    }

    /**
     * Wrapper for JavaAttribute inner element attributes (in case JavaAttribute
     * is array)
     */
    static class AttributeIfcWrapper implements AttributeIfc {

        private final OpenType<?> elementOpenType;

        public AttributeIfcWrapper(OpenType<?> elementOpenType) {
            this.elementOpenType = elementOpenType;
        }

        @Override
        public String getAttributeYangName() {
            return null;
        }

        @Override
        public String getNullableDescription() {
            return null;
        }

        @Override
        public String getNullableDefault() {
            return null;
        }

        @Override
        public String getUpperCaseCammelCase() {
            return null;
        }

        @Override
        public String getLowerCaseCammelCase() {
            return null;
        }

        @Override
        public OpenType<?> getOpenType() {
            return elementOpenType;
        }

    }
}
