package org.opendaylight.controller.md.sal.binding.compat;

import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
@Deprecated
public class HydrogenMountProvisionServiceAdapter extends HydrogenMountPointServiceAdapter implements MountProviderService {

    public HydrogenMountProvisionServiceAdapter(MountPointService mountService) {
        super(mountService);
    }

    @Override
    public MountProviderInstance createMountPoint(InstanceIdentifier<?> path) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MountProviderInstance createOrGetMountPoint(InstanceIdentifier<?> path) {
        return getMountPoint(path);
    }

    @Override
    public ListenerRegistration<MountProvisionListener> registerProvisionListener(final MountProvisionListener listener) {
        return new ListenerRegistration<MountProvisionListener>() {

            @Override
            public MountProvisionListener getInstance() {
                return listener;
            }

            @Override
            public void close() {
            }
        };
    }

}
