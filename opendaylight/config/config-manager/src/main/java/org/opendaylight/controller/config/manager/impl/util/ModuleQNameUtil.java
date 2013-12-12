/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.yang.binding.annotations.ModuleQName;
import org.osgi.framework.BundleContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ModuleQNameUtil {

    public static Set<String> getQNames(Map<String, Entry<ModuleFactory, BundleContext>> resolved) {
        Set<String> result = new HashSet<>();
        for (Entry<ModuleFactory, BundleContext> entry : resolved.values()) {
            Class<?> inspected = entry.getKey().getClass();
            if (inspected.isInterface()) {
                throw new IllegalArgumentException("Unexpected interface " + inspected);
            }
            ModuleQName annotation = null;
            while(annotation == null && inspected != null) {
                annotation = inspected.getAnnotation(ModuleQName.class);
                inspected = inspected.getSuperclass();
            }
            if (annotation != null) {
                // FIXME
                String qName = "(" + annotation.namespace() + "?revision=" + annotation.revision() + ")" + annotation.name();
                result.add(qName);
            }
        }
        return result;
    }

}
