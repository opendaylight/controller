package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeContext {

    private final InstanceIdentifierContext context;
    private final NormalizedNode<?,?> data;

    public NormalizedNodeContext(final InstanceIdentifierContext context, final NormalizedNode<?, ?> data) {
        this.context = context;
        this.data = data;
    }

    public InstanceIdentifierContext getInstanceIdentifierContext() {
        return context;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }
}
