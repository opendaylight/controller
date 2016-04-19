/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodecFactory;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public final class RuntimeMappingModule extends AbstractRuntimeMappingModule {
    private static final long WAIT_IN_MINUTES = 5;

    private BundleContext bundleContext;

    public RuntimeMappingModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RuntimeMappingModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final RuntimeMappingModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkNotNull(bundleContext);
        // Add custom validation for module attributes here.
    }

    @Override
    public boolean canReuseInstance(final AbstractRuntimeMappingModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // This is kind of ugly - you might cringe (you've been warned). The BindingToNormalizedNodeCodec
        // instance is advertised via blueprint so ideally we'd obtain it from the OSGi service registry.
        // The config yang service identity declares the concrete BindingToNormalizedNodeCodec class
        // and not an interface as the java-class so we must return a BindingToNormalizedNodeCodec instance.
        // However we can't cast the instance obtained from the service registry to
        // BindingToNormalizedNodeCodec b/c Aries may register a proxy if there are interceptors defined.
        // By default karaf ships with the org.apache.aries.quiesce.api bundle which automatically adds
        // an interceptor that adds stat tracking for service method calls. While this can be disabled, we
        // shouldn't rely on it.
        //
        // Therefore we store a static instance in the BindingToNormalizedNodeCodecFactory which is created
        // by blueprint via newInstance. We obtain the static instance here and busy wait if not yet available.

        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MINUTES) <= WAIT_IN_MINUTES) {
            BindingToNormalizedNodeCodec instance = BindingToNormalizedNodeCodecFactory.getInstance();
            if(instance != null) {
                return instance;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        throw new IllegalStateException("Could not obtain the BindingToNormalizedNodeCodec instance after " +
                WAIT_IN_MINUTES + " minutes.");
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
