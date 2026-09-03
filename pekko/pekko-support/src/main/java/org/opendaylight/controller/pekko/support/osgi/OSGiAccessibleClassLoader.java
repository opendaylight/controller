/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.osgi;

import com.google.common.annotations.Beta;
import org.apache.pekko.osgi.BundleDelegatingClassLoader;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.AccessControllerCompat;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * A ClassLoader delegating to a {@link Bundle}'s class loader.
 *
 * @since 14.0.0
 */
@Beta
@NonNullByDefault
@SuppressWarnings("exports")
public final class OSGiAccessibleClassLoader extends BundleDelegatingClassLoader {
    private OSGiAccessibleClassLoader(final Bundle bundle) {
        // TODO: why do we need the TCCL here?
        super(bundle, Thread.currentThread().getContextClassLoader());
    }

    /**
     * {@return a new instance backed by specified {@link BundleContext}}
     *
     * @param bundleContext the bundle context
     */
    public static OSGiAccessibleClassLoader of(final BundleContext bundleContext) {
        return AccessControllerCompat.get(() -> new OSGiAccessibleClassLoader(bundleContext.getBundle()));
    }
}
