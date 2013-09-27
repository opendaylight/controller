/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yangtools.yang.model.api.Module;

import com.google.common.base.Preconditions;

public class YangModelSearchUtils {

    public static Map<String, Module> mapModulesByNames(
            Collection<Module> modules) {
        Map<String, Module> result = new HashMap<>();
        for (Module m : modules) {
            String moduleName = m.getName();
            Preconditions.checkArgument(
                    result.containsKey(moduleName) == false,
                    "Two modules have same name " + moduleName);
            result.put(moduleName, m);
        }
        return result;
    }
}
