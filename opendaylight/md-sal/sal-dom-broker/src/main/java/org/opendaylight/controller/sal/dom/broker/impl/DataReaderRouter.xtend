package org.opendaylight.controller.sal.dom.broker.impl

import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.CompositeNode

class DataReaderRouter extends AbstractDataReadRouter<InstanceIdentifier, CompositeNode> {

    override protected merge(InstanceIdentifier path, Iterable<CompositeNode> data) {
        return data.iterator.next
    }

}
