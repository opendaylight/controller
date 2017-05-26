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
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BindingDOMMountPointServiceAdapter implements MountPointService {

    public static final Logger LOG = LoggerFactory.getLogger(BindingDOMMountPointServiceAdapter.class);

    private final BindingToNormalizedNodeCodec codec;
    private final DOMMountPointService mountService;
    private final LoadingCache<DOMMountPoint, BindingMountPointAdapter> bindingMountpoints = CacheBuilder.newBuilder()
            .weakKeys().build(new CacheLoader<DOMMountPoint, BindingMountPointAdapter>() {

                @Override
                public BindingMountPointAdapter load(DOMMountPoint key) throws Exception {
                    return new BindingMountPointAdapter(codec,key);
                }
            });

    public BindingDOMMountPointServiceAdapter(DOMMountPointService mountService,BindingToNormalizedNodeCodec codec) {
        this.codec = codec;
        this.mountService = mountService;
    }

    @Override
    public Optional<MountPoint> getMountPoint(InstanceIdentifier<?> mountPoint) {

        YangInstanceIdentifier domPath = codec.toYangInstanceIdentifierBlocking(mountPoint);
        Optional<DOMMountPoint> domMount = mountService.getMountPoint(domPath);
        if(domMount.isPresent()) {
            return Optional.<MountPoint>fromNullable(bindingMountpoints.getUnchecked(domMount.get()));
        }
        return Optional.absent();
    }

    @Override
    public <T extends MountPointListener> ListenerRegistration<T> registerListener(InstanceIdentifier<?> path,
            T listener) {
        return new BindingDOMMountPointListenerAdapter<>(listener, codec, mountService);
    }

}
