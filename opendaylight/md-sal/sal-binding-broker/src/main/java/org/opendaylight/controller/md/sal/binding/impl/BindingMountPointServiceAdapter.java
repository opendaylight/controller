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
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingMountPointServiceAdapter implements MountPointService {
    public static final Logger LOG = LoggerFactory.getLogger(BindingMountPointServiceAdapter.class);

    private final org.opendaylight.mdsal.binding.api.MountPointService delegate;
    private final LoadingCache<org.opendaylight.mdsal.binding.api.MountPoint, BindingMountPointAdapter>
        bindingMountpoints = CacheBuilder.newBuilder()
            .weakKeys().build(new CacheLoader<org.opendaylight.mdsal.binding.api.MountPoint,
                    BindingMountPointAdapter>() {
                @Override
                public BindingMountPointAdapter load(org.opendaylight.mdsal.binding.api.MountPoint key) {
                    return new BindingMountPointAdapter(key);
                }
            });

    public BindingMountPointServiceAdapter(org.opendaylight.mdsal.binding.api.MountPointService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<MountPoint> getMountPoint(InstanceIdentifier<?> mountPointPath) {
        Optional<org.opendaylight.mdsal.binding.api.MountPoint> delegateMountPoint =
                delegate.getMountPoint(mountPointPath);
        if (delegateMountPoint.isPresent()) {
            return Optional.<MountPoint>fromNullable(bindingMountpoints.getUnchecked(delegateMountPoint.get()));
        }
        return Optional.absent();
    }

    @Override
    public <T extends MountPointListener> ListenerRegistration<T> registerListener(InstanceIdentifier<?> path,
            T listener) {
        return new BindingMountPointListenerAdapter<>(path, listener, delegate);
    }
}
