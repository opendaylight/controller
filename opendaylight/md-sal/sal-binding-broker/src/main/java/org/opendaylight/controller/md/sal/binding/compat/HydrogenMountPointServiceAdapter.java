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

    public HydrogenMountPointServiceAdapter(MountPointService mountService) {
        delegate = mountService;
    }

    private LoadingCache<MountPoint, HydrogenMountInstanceAdapter> mountAdapters = CacheBuilder.newBuilder().weakKeys()
            .build(new CacheLoader<MountPoint, HydrogenMountInstanceAdapter>() {

                @Override
                public HydrogenMountInstanceAdapter load(MountPoint key) throws Exception {
                    return new HydrogenMountInstanceAdapter(key);
                }
            });

    @Override
    public HydrogenMountInstanceAdapter getMountPoint(InstanceIdentifier<?> path) {
        Optional<MountPoint> mount = delegate.getMountPoint(path);
        if (mount.isPresent()) {
            return mountAdapters.getUnchecked(mount.get());
        }
        return null;
    }

    MountPointService getDelegate() {
        return delegate;
    }
}
