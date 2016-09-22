/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.md.sal.config.impl.cluster.singleton.service.rev160718;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import java.lang.reflect.Method;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring but remains for backwards compatibility until downstream users
 *             of the provided config system service are converted to blueprint.
 */
@Deprecated
public class ClusterSingletonServiceProviderModule extends AbstractClusterSingletonServiceProviderModule {
    private BundleContext bundleContext;

    public ClusterSingletonServiceProviderModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ClusterSingletonServiceProviderModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
            final ClusterSingletonServiceProviderModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }


    @Override
    public AutoCloseable createInstance() {
        final WaitingServiceTracker<ClusterSingletonServiceProvider> tracker = WaitingServiceTracker
                .create(ClusterSingletonServiceProvider.class, bundleContext);
        final ClusterSingletonServiceProvider service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        // Create a proxy to override close to close the ServiceTracker. The actual DOMClusterSingletonServiceProvider
        // instance will be closed via blueprint.
        return Reflection.newProxy(AutoCloseableDOMClusterSingletonServiceProvider.class,
                new AbstractInvocationHandler() {
                    @Override
                    protected Object handleInvocation(final Object proxy, final Method method, final Object[] args)
                            throws Throwable {
                        if (method.getName().equals("close")) {
                            tracker.close();
                            return null;
                        } else {
                            return method.invoke(service, args);
                        }
                    }
                });
    }

    void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private interface AutoCloseableDOMClusterSingletonServiceProvider
            extends ClusterSingletonServiceProvider, AutoCloseable {
    }
}