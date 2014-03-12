package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncReadTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DOMDataReadTransaction extends AsyncReadTransaction<InstanceIdentifier, NormalizedNode<?, ?>> {

}
