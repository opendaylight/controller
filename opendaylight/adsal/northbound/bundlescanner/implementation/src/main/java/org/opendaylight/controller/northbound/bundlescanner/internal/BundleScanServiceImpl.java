/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.bundlescanner.internal;

import java.util.List;
import java.util.Set;

import org.opendaylight.controller.northbound.bundlescanner.IBundleScanService;
import org.osgi.framework.BundleContext;

public class BundleScanServiceImpl implements IBundleScanService {

    public BundleScanServiceImpl() {}


    @Override
    public List<Class<?>> getAnnotatedClasses(BundleContext context,
            String[] annotations,
            Set<String> excludes,
            boolean includeDependentBundleClasses)
    {
        return BundleScanner.getInstance().getAnnotatedClasses(
                context, annotations, excludes, includeDependentBundleClasses);
    }

}
