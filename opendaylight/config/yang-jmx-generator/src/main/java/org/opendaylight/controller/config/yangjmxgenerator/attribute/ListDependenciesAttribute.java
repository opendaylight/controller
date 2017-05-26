/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import java.util.List;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.mdsal.binding.model.util.Types;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class ListDependenciesAttribute extends AbstractDependencyAttribute {

    public ListDependenciesAttribute(final DataSchemaNode attrNode, final ServiceInterfaceEntry sie, final boolean mandatory, final String nullableDescription) {
        super(attrNode, sie, mandatory, nullableDescription);
    }

    @Override
    public Type getType() {
        return Types.parameterizedTypeFor(Types.typeForClass(List.class), Types.typeForClass(ObjectName.class));
    }

    @Override
    public ArrayType<?> getOpenType() {
        final OpenType<?> innerOpenType = SimpleType.OBJECTNAME;
        return ListAttribute.constructArrayType(innerOpenType);
    }

}
