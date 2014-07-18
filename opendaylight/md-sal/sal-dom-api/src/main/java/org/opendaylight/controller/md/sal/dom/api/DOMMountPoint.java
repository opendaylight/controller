package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface DOMMountPoint extends Identifiable<InstanceIdentifier> {

    <T extends DOMService> Optional<T> getService(Class<T> cls);

    SchemaContext getSchemaContext();
}
