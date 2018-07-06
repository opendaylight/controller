/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BindingMountPointAdapter implements MountPoint {

    private org.opendaylight.mdsal.binding.api.MountPoint delegate;
    private LoadingCache<Class<? extends BindingService>, Optional<BindingService>> services;

    public BindingMountPointAdapter(final org.opendaylight.mdsal.binding.api.MountPoint delegate) {
        this.delegate = delegate;
        services = CacheBuilder.newBuilder().build(new BindingAdapterLoader() {
            @Override
            protected org.opendaylight.mdsal.binding.api.BindingService getDelegate(
                    Class<? extends org.opendaylight.mdsal.binding.api.BindingService> reqDeleg) {
                return delegate.getService(reqDeleg).orNull();
            }
        });
    }

    @Override
    public InstanceIdentifier<?> getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public <T extends BindingService> Optional<T> getService(Class<T> service) {
        Optional<BindingService> potential = services.getUnchecked(service);
        if (potential.isPresent()) {
            return Optional.of(service.cast(potential.get()));
        }
        return Optional.absent();
    }

}
