/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public final class RuntimeMappingModule extends AbstractRuntimeMappingModule {
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
    public AutoCloseable createInstance() {
        // We need to return the concrete BindingToNormalizedNodeCodec instance for backwards compatibility
        // for CSS users that inject the binding-dom-mapping-service.
        final WaitingServiceTracker<BindingToNormalizedNodeCodec> tracker =
                WaitingServiceTracker.create(BindingToNormalizedNodeCodec.class, bundleContext);
        final BindingToNormalizedNodeCodec service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        // Ideally we would close the ServiceTracker via the returned AutoCloseable but then we'd have to
        // proxy the BindingToNormalizedNodeCodec instance which is problematic. It's OK to close the
        // ServiceTracker here.
        tracker.close();
        return service;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
