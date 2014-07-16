package org.opendaylight.controller.md.sal.dom.broker.impl.mount;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.md.sal.dom.broker.spi.mount.SimpleDOMMountPoint;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

public class DOMMountPointServiceImpl implements DOMMountPointService {

    Map<InstanceIdentifier, SimpleDOMMountPoint> mountPoints = new HashMap<>();

    @Override
    public Optional<DOMMountPoint> getMountPoint(final InstanceIdentifier path) {
        return Optional.<DOMMountPoint>fromNullable(mountPoints.get(path));
    }

    @Override
    public DOMMountPointBuilder createMountPoint(final InstanceIdentifier path) {
        Preconditions.checkState(!mountPoints.containsKey(path), "Mount point already exists");
        return new DOMMountPointBuilderImpl(path);
    }

    public ObjectRegistration<DOMMountPoint> registerMountPoint(final SimpleDOMMountPoint mountPoint) {
        synchronized (mountPoints) {
            Preconditions.checkState(!mountPoints.containsKey(mountPoint.getIdentifier()), "Mount point already exists");
            mountPoints.put(mountPoint.getIdentifier(), mountPoint);
        }
        return null;
    }



    public class DOMMountPointBuilderImpl implements DOMMountPointBuilder {

        ClassToInstanceMap<DOMService> services = MutableClassToInstanceMap.create();
        private SimpleDOMMountPoint mountPoint;
        private final InstanceIdentifier path;
        private SchemaContext schemaContext;

        public DOMMountPointBuilderImpl(final InstanceIdentifier path) {
            this.path = path;
        }

        @Override
        public <T extends DOMService> DOMMountPointBuilder addService(final Class<T> type, final T impl) {
            services.putInstance(type, impl);
            return this;
        }

        @Override
        public DOMMountPointBuilder addInitialSchemaContext(final SchemaContext ctx) {
            schemaContext = ctx;
            return this;
        }

        @Override
        public ObjectRegistration<DOMMountPoint> build() {
            Preconditions.checkState(mountPoint == null, "Mount point is already built.");
            mountPoint = SimpleDOMMountPoint.create(path, services,schemaContext);
            return registerMountPoint(mountPoint);
        }
    }
}
