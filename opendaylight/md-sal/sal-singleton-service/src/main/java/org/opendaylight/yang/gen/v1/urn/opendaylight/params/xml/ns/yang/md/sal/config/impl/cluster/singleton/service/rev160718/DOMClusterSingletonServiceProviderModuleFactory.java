/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.md.sal.config.impl.cluster.singleton.service.rev160718;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring but remains for backwards compatibility until downstream users
 *             of the provided config system service are converted to blueprint.
 */
@Deprecated
public class DOMClusterSingletonServiceProviderModuleFactory extends AbstractDOMClusterSingletonServiceProviderModuleFactory {
    @Override
    public DOMClusterSingletonServiceProviderModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            DOMClusterSingletonServiceProviderModule oldModule, AutoCloseable oldInstance, BundleContext bundleContext) {
        DOMClusterSingletonServiceProviderModule module = super.instantiateModule(instanceName, dependencyResolver, oldModule,
                oldInstance, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public DOMClusterSingletonServiceProviderModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            BundleContext bundleContext) {
        DOMClusterSingletonServiceProviderModule module = super.instantiateModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }
}
