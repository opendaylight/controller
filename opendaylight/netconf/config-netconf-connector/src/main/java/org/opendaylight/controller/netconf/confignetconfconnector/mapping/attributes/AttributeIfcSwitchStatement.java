/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes;

import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;

public abstract class AttributeIfcSwitchStatement<T> {

    public T switchAttribute(AttributeIfc attributeIfc) {

        if (attributeIfc instanceof JavaAttribute) {
            return caseJavaAttribute((JavaAttribute) attributeIfc);
        } else if (attributeIfc instanceof DependencyAttribute) {
            return caseDependencyAttribute((DependencyAttribute) attributeIfc);
        } else if (attributeIfc instanceof ListAttribute) {
            return caseListAttribute((ListAttribute) attributeIfc);
        } else if (attributeIfc instanceof TOAttribute) {
            return caseTOAttribute((TOAttribute) attributeIfc);
        }

        throw new IllegalArgumentException("Unknown attribute type " + attributeIfc.getClass() + ", " + attributeIfc);
    }

    protected abstract T caseJavaAttribute(JavaAttribute attributeIfc);

    protected abstract T caseDependencyAttribute(DependencyAttribute attributeIfc);

    protected abstract T caseTOAttribute(TOAttribute attributeIfc);

    protected abstract T caseListAttribute(ListAttribute attributeIfc);
}
