/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.services_implementation.internal;

import java.lang.ref.WeakReference;

import org.jboss.marshalling.ContextClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassResolver extends ContextClassResolver {
    private WeakReference<ClassLoader> osgiClassLoader = null;
    private static final Logger logger = LoggerFactory.getLogger(ClassResolver.class);

    public ClassResolver() {
        ClassLoader cl = this.getClass()
                .getClassLoader();
        if (cl != null) {
            this.osgiClassLoader = new WeakReference<ClassLoader>(cl);
            logger.trace("Acquired weak reference to OSGi classLoader {}", cl);
        }
    }

    @Override
    protected ClassLoader getClassLoader() {
        ClassLoader ret = null;
        if (this.osgiClassLoader != null) {
            ret = this.osgiClassLoader.get();
            if (ret != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Returning OSGi class loader {}", ret);
                }
                return ret;
            }
        }

        logger.warn("Could not resolve classloader!");
        return ret;
    }
}
