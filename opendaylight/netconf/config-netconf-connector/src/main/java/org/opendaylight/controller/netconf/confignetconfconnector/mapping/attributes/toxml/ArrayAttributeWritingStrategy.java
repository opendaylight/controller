/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml;

import java.util.List;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.w3c.dom.Element;

public class ArrayAttributeWritingStrategy implements AttributeWritingStrategy {

    private final AttributeWritingStrategy innnerStrategy;

    public ArrayAttributeWritingStrategy(AttributeWritingStrategy innerStrategy) {
        this.innnerStrategy = innerStrategy;
    }

    @Override
    public void writeElement(Element parentElement, String namespace, Object value) {
        NetconfUtil.checkType(value, List.class);

        for (Object innerObject : ((List<?>) value)) {
            innnerStrategy.writeElement(parentElement, namespace, innerObject);
        }

    }

}
