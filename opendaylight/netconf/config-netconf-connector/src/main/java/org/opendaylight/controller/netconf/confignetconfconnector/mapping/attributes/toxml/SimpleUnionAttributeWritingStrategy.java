/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.w3c.dom.Document;

public class SimpleUnionAttributeWritingStrategy extends SimpleAttributeWritingStrategy {

    /**
     * @param document
     * @param key
     */
    public SimpleUnionAttributeWritingStrategy(Document document, String key) {
        super(document, key);
    }

    protected Object preprocess(Object value) {
        NetconfUtil.checkType(value, Map.class);
        Preconditions.checkArgument(((Map<?, ?>)value).size() == 1, "Unexpected number of values in %s, expected 1", value);
        Object listOfStrings = ((Map<?, ?>) value).values().iterator().next();
        NetconfUtil.checkType(listOfStrings, List.class);

        StringBuilder b = new StringBuilder();
        for (Object character: (List<?>)listOfStrings) {
            NetconfUtil.checkType(character, String.class);
            b.append(character);
        }

        return b.toString();
    }

}
