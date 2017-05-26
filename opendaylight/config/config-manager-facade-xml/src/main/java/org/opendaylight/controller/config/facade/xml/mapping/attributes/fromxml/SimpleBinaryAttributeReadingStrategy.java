/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import java.util.List;

public class SimpleBinaryAttributeReadingStrategy extends SimpleAttributeReadingStrategy {

    public SimpleBinaryAttributeReadingStrategy(String nullableDefault) {
        super(nullableDefault);
    }

    @Override
    protected Object postprocessParsedValue(String textContent) {
        BaseEncoding en = BaseEncoding.base64();
        byte[] decode = en.decode(textContent);
        List<String> parsed = Lists.newArrayListWithCapacity(decode.length);
        for (byte b : decode) {
            parsed.add(Byte.toString(b));
        }
        return parsed;
    }

    @Override
    protected Object postprocessNullableDefault(String nullableDefault) {
        return nullableDefault == null ? null : postprocessParsedValue(nullableDefault);
    }
}
