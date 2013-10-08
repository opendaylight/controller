/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.stat;

import org.osgi.framework.BundleContext;

/**
 * Subset of {@link org.osgi.framework.BundleContext}
 */
public interface ConfigProvider {
    /**
     * Returns the value of the specified property. If the key is not found in
     * the Framework properties, the system properties are then searched. The
     * method returns {@code null} if the property is not found.
     *
     * <p>
     * All bundles must have permission to read properties whose names start
     * with &quot;org.osgi.&quot;.
     *
     * @param key
     *            The name of the requested property.
     * @return The value of the requested property, or {@code null} if the
     *         property is undefined.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             {@code PropertyPermission} to read the property, and the Java
     *             Runtime Environment supports permissions.
     */
    String getProperty(String key);

    public static class ConfigProviderImpl implements ConfigProvider {
        private final BundleContext context;

        public ConfigProviderImpl(BundleContext context) {
            this.context = context;
        }

        @Override
        public String getProperty(String key) {
            return context.getProperty(key);
        }

        @Override
        public String toString() {
            return "ConfigProviderImpl{" + "context=" + context + '}';
        }
    }

}
