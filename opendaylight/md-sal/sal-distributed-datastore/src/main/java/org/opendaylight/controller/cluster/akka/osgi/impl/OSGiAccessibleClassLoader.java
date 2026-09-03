/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import org.apache.pekko.osgi.BundleDelegatingClassLoader;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.AccessControllerCompat;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@NonNullByDefault
final class OSGiAccessibleClassLoader extends BundleDelegatingClassLoader {
    private OSGiAccessibleClassLoader(final Bundle bundle) {
        super(bundle, Thread.currentThread().getContextClassLoader());
    }

    static OSGiAccessibleClassLoader of(final BundleContext bundleContext) {
        return AccessControllerCompat.get(() -> new OSGiAccessibleClassLoader(bundleContext.getBundle()));
    }
}
