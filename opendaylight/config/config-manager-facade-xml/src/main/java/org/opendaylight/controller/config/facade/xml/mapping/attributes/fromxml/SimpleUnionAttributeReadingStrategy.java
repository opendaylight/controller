/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;

public class SimpleUnionAttributeReadingStrategy extends SimpleAttributeReadingStrategy {

    private final String key;

    public SimpleUnionAttributeReadingStrategy(String nullableDefault, String key) {
        super(nullableDefault);
        this.key = key;
    }

    @Override
    protected Object postprocessParsedValue(String textContent) {
        char[] charArray = textContent.toCharArray();
        List<String> chars = Lists.newArrayListWithCapacity(charArray.length);

        for (char c : charArray) {
            chars.add(Character.toString(c));
        }

        Map<String, Object> map = Maps.newHashMap();
        map.put(key, chars);
        return map;
    }

    @Override
    protected Object postprocessNullableDefault(String nullableDefault) {
        return nullableDefault == null ? null : postprocessParsedValue(nullableDefault);
    }
}
