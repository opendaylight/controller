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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SimpleCodeStringProvider maintains a mapping between codes and Strings. When a string is saved without
 * a code it automatically assigns an auto-incrementing code to the String. This helps ensure that NormalizedNode
 * streams encoded by a Lithium NormalizedNodeOutputStreamWriter can be properly read.
 */
public class SimpleCodeStringProvider implements CodeStringProvider {

    private final Map<Integer, String> codeStringMap = new HashMap<>();

    @Nullable
    @Override
    public String getString(Integer code) {
        Preconditions.checkNotNull(code);
        return codeStringMap.get(code);
    }

    @Override
    public void saveString(@Nonnull String str, Integer code) {
        Preconditions.checkNotNull(str);

        if(code == null){
            code = codeStringMap.size();
        }
        codeStringMap.put(code, str);
    }
}
