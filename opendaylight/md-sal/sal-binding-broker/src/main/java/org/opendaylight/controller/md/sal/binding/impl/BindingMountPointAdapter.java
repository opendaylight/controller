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
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BindingMountPointAdapter implements MountPoint {

    private final InstanceIdentifier<?> identifier;
    private LoadingCache<Class<? extends BindingService>, Optional<BindingService>> services;

    public BindingMountPointAdapter(final BindingToNormalizedNodeCodec codec, final DOMMountPoint domMountPoint) {
        identifier = codec.getCodecRegistry().fromYangInstanceIdentifier(domMountPoint.getIdentifier());
        services = CacheBuilder.newBuilder().build(new BindingDOMAdapterLoader(codec) {

            @Override
            protected DOMService getDelegate(Class<? extends DOMService> reqDeleg) {
                return domMountPoint.getService(reqDeleg).orNull();
            }
        });
    }

    @Override
    public InstanceIdentifier<?> getIdentifier() {
        return identifier;
    }

    @Override
    public <T extends BindingService> Optional<T> getService(Class<T> service) {
        Optional<BindingService> potential = services.getUnchecked(service);
        if(potential.isPresent()) {
            return Optional.of(service.cast(potential.get()));
        }
        return Optional.absent();
    }

}
