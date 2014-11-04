/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.bundlescanner;

import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;

/**
 * The bundle scan service provides services which allow introspection of
 * bundle classes for detecting annotated classes. The scanning is performed
 * when a bundle is RESOLVED.
 */
public interface IBundleScanService {
    /**
     * The list of annotations to be scanned
     */
    public final String[] ANNOTATIONS_TO_SCAN = {
        "javax.xml.bind.annotation.*", // JAXB annotatinos
        "javax.ws.rs.*"                // JAX-RS annotatinos
    };


    public List<Class<?>> getAnnotatedClasses(
            BundleContext context,
            String[] annotations,
            Set<String> excludes,
            boolean includeDependentBundleClasses);
}
