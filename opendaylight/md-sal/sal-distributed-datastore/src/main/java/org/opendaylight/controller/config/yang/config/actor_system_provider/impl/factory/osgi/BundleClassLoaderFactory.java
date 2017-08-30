/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.actor_system_provider.impl.factory.osgi;

import akka.osgi.BundleDelegatingClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.osgi.framework.BundleContext;

public class BundleClassLoaderFactory {

    public static ClassLoader createClassLoader(final BundleContext bundleContext) {
        return AccessController
                .doPrivileged((PrivilegedAction<BundleDelegatingClassLoader>) () -> new BundleDelegatingClassLoader(
                        bundleContext.getBundle(), Thread.currentThread().getContextClassLoader()));
    }
}
