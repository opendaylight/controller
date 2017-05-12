/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml;

import com.google.common.base.Preconditions;
import java.util.Map;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.w3c.dom.Document;

public class SimpleCompositeAttributeWritingStrategy extends SimpleAttributeWritingStrategy {

    /**
     * @param document
     * @param key
     */
    public SimpleCompositeAttributeWritingStrategy(final Document document, final String key) {
        super(document, key);
    }

    protected Object preprocess(final Object value) {
        Util.checkType(value, Map.class);
        Preconditions.checkArgument(((Map<?, ?>)value).size() == 1, "Unexpected number of values in %s, expected 1", value);
        return ((Map<?, ?>)value).values().iterator().next();
    }

}
