/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.mount.MountService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Deprecated
public class HydrogenMountPointServiceAdapter implements MountService {

    private final MountPointService delegate;

    public HydrogenMountPointServiceAdapter(final MountPointService mountService) {
        delegate = mountService;
    }

    private final LoadingCache<MountPoint, HydrogenMountInstanceAdapter> mountAdapters = CacheBuilder.newBuilder().weakKeys()
            .build(new CacheLoader<MountPoint, HydrogenMountInstanceAdapter>() {

                @Override
                public HydrogenMountInstanceAdapter load(final MountPoint key) throws Exception {
                    return new HydrogenMountInstanceAdapter(key);
                }
            });

    @Override
    public HydrogenMountInstanceAdapter getMountPoint(final InstanceIdentifier<?> path) {
        final Optional<MountPoint> mount = delegate.getMountPoint(path);
        if (mount.isPresent()) {
            return mountAdapters.getUnchecked(mount.get());
        }
        return null;
    }

    MountPointService getDelegate() {
        return delegate;
    }
}
