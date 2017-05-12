/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import java.util.List;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.w3c.dom.Document;

public class SimpleBinaryAttributeWritingStrategy extends SimpleAttributeWritingStrategy {

    /**
     * @param document
     * @param key
     */
    public SimpleBinaryAttributeWritingStrategy(final Document document, final String key) {
        super(document, key);
    }

    @Override
    protected Object preprocess(final Object value) {
        Util.checkType(value, List.class);
        BaseEncoding en = BaseEncoding.base64();

        List<?> list = (List<?>) value;
        byte[] decoded = new byte[list.size()];
        int i = 0;
        for (Object bAsStr : list) {
            Preconditions.checkArgument(bAsStr instanceof String, "Unexpected inner value for %s, expected string", value);
            byte b = Byte.parseByte((String) bAsStr);
            decoded[i++] = b;
        }

        return en.encode(decoded);
    }

}
