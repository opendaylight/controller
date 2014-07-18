package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;


public interface DOMMountPointService extends BrokerService {

    Optional<DOMMountPoint> getMountPoint(InstanceIdentifier path);

    DOMMountPointBuilder createMountPoint(InstanceIdentifier path);


    public interface DOMMountPointBuilder {

        <T extends DOMService> DOMMountPointBuilder addService(Class<T> type,T impl);

        DOMMountPointBuilder addInitialSchemaContext(SchemaContext ctx);

        ObjectRegistration<DOMMountPoint> build();
    }
}
