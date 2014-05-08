package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncReadOnlyTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DOMDataReadOnlyTransaction extends DOMDataReadTransaction, AsyncReadOnlyTransaction<InstanceIdentifier, NormalizedNode<?, ?>> {

}
