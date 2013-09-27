/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public abstract class AbstractAttribute implements AttributeIfc {
    private final String attributeYangName, upperCaseCammelCase,
            lowerCaseCammelCase;
    private final DataSchemaNode node;

    private static String getLocalName(DataSchemaNode attrNode) {
        return attrNode.getQName().getLocalName();
    }

    AbstractAttribute(DataSchemaNode attrNode) {
        this.attributeYangName = getLocalName(attrNode);
        this.node = attrNode;
        this.upperCaseCammelCase = ModuleMXBeanEntry.findJavaNamePrefix(node);
        this.lowerCaseCammelCase = ModuleMXBeanEntry.findJavaParameter(node);
    }

    @Override
    public String getAttributeYangName() {
        return attributeYangName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AbstractAttribute))
            return false;

        AbstractAttribute that = (AbstractAttribute) o;

        if (attributeYangName != null ? !attributeYangName
                .equals(that.attributeYangName)
                : that.attributeYangName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return attributeYangName != null ? attributeYangName.hashCode() : 0;
    }

    /**
     *
     * @return Yang name converted to cammel case, starting with a capital
     *         letter. For details see
     *         {@link ModuleMXBeanEntry#findJavaNamePrefix(org.opendaylight.yangtools.yang.model.api.SchemaNode)}
     */
    @Override
    public String getUpperCaseCammelCase() {
        return upperCaseCammelCase;
    }

    public String getLowerCaseCammelCase() {
        return lowerCaseCammelCase;
    }
}
