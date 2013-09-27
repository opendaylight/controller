/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.yangtools.binding.generator.util.BindingGeneratorUtil;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Maps from module namespaces to java package names using a Map<String,
 * String>, where key is namespace prefix and value is package that replaces
 * matched prefix.
 */
public class PackageTranslator {
    private final Map<String, String> namespacePrefixToPackageMap;

    public PackageTranslator(Map<String, String> namespacePrefixToPackageMap) {
        this.namespacePrefixToPackageMap = namespacePrefixToPackageMap;
    }

    /**
     * Based on mapping, find longest matching key and return value plus the
     * remaining part of namespace, with colons replaced by dots. Example:
     * Mapping [ 'urn:opendaylight:params:xml:ns:yang:controller' :
     * 'org.opendaylight.controller'] and module with namespace
     * 'urn:opendaylight:params:xml:ns:yang:controller:threads:api' will result
     * in 'org.opendaylight.controller.threads.api' .
     *
     * @throws IllegalStateException
     *             if there is no mapping found.
     */
    public String getPackageName(Module module) {
        Entry<String, String> longestMatch = null;
        int longestMatchLength = 0;
        String namespace = module.getNamespace().toString();
        for (Entry<String, String> entry : namespacePrefixToPackageMap
                .entrySet()) {
            if (namespace.startsWith(entry.getKey())
                    && entry.getKey().length() > longestMatchLength) {
                longestMatch = entry;
                longestMatchLength = entry.getKey().length();
            }
        }
        if (longestMatch != null) {
            return longestMatch.getValue()
                    + sanitizePackage(namespace.substring(longestMatchLength));
        } else {
            return BindingGeneratorUtil.moduleNamespaceToPackageName(module);
        }
    }

    // TODO add to PackageTranslator
    private static String sanitizePackage(String namespace) {
        namespace = namespace.replace("://", ".");
        namespace = namespace.replace("/", ".");
        namespace = namespace.replace(":", ".");
        namespace = namespace.replace("-", "_");
        namespace = namespace.replace("@", ".");
        namespace = namespace.replace("$", ".");
        namespace = namespace.replace("#", ".");
        namespace = namespace.replace("'", ".");
        namespace = namespace.replace("*", ".");
        namespace = namespace.replace("+", ".");
        namespace = namespace.replace(",", ".");
        namespace = namespace.replace(";", ".");
        namespace = namespace.replace("=", ".");
        return namespace;
    }
}
