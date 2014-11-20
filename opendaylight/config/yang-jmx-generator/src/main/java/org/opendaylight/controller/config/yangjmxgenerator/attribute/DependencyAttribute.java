/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import javax.management.ObjectName;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.yangtools.binding.generator.util.Types;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class DependencyAttribute extends AbstractDependencyAttribute {


    public DependencyAttribute(DataSchemaNode attrNode,
            ServiceInterfaceEntry sie, boolean mandatory,
            String nullableDescription) {
        super(attrNode, sie, mandatory, nullableDescription);
    }

    @Override
    public Type getType() {
        return Types.typeForClass(ObjectName.class);
    }

    @Override
    public SimpleType<?> getOpenType() {
        return SimpleType.OBJECTNAME;
    }

}
