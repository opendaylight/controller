/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.sal.common.util.osgi.WaitingServiceTracker;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.osgi.framework.BundleContext;

/**
 *
**/
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
    public java.lang.AutoCloseable createInstance() {
        // The service is provided via blueprint so wait for and return it here for backwards compatibility.
        final WaitingServiceTracker<BindingNormalizedNodeSerializer> tracker = WaitingServiceTracker.create(
                BindingNormalizedNodeSerializer.class, bundleContext);
        BindingToNormalizedNodeCodec codec = (BindingToNormalizedNodeCodec) tracker.waitForService(
                WaitingServiceTracker.FIVE_MINUTES);

        // We can't wrap the BindingToNormalizedNodeCodec to also close the tracker so we'll close it here.
        // This is OK since the BindingToNormalizedNodeCodec doesn't do anything on close so if the
        // blueprint was destroyed first and closed the BindingToNormalizedNodeCodec, it wouldn't hurt
        // anything.
        tracker.close();
        return codec;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
