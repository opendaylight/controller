/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;

/**
 * SimpleStringCodeProvider starts with an empty mapping between Strings and codes and
 * generates a simple auto-incrementing integer as the code for the string when it is
 * requested to create a code.
 */
public class SimpleStringCodeProvider implements StringCodeProvider {

    private final Map<String, Integer> stringToCodeMap = new HashMap<>();

    private final NormalizedNodeOutputStreamWriterVersion normalizedNodeOutputStreamWriterVersion;

    public SimpleStringCodeProvider(NormalizedNodeOutputStreamWriterVersion normalizedNodeOutputStreamWriterVersion){
        this.normalizedNodeOutputStreamWriterVersion = normalizedNodeOutputStreamWriterVersion;
    }

    @Override
    public Integer getCode(String str) {
        Preconditions.checkNotNull(str);
        return stringToCodeMap.get(str);
    }

    @Override
    public Integer createCode(String str) {
        Preconditions.checkNotNull(str);
        Preconditions.checkState(getCode(str) == null, "Code already exists for string = " + str );
        int code = stringToCodeMap.size();
        stringToCodeMap.put(str, code);
        return code;
    }

    @Override
    public boolean isCompatibleWith(NormalizedNodeOutputStreamWriterVersion version) {
        return this.normalizedNodeOutputStreamWriterVersion.ordinal() <= version.ordinal();
    }

}
