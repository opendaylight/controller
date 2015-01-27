package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public interface DOMDataChangePublishService {

    <L extends DOMDataListener> ListenerRegistration<L> registerDataChangeListener(LogicalDatastoreType type, YangInstanceIdentifier subtree,L listener);

}
