package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public class NormalizedNodeContext {

    private final InstanceIdentifierContext<? extends SchemaNode> context;
    private final NormalizedNode<?,?> data;

    public NormalizedNodeContext(final InstanceIdentifierContext<? extends SchemaNode> context, final NormalizedNode<?, ?> data) {
        this.context = context;
        this.data = data;
    }

    public InstanceIdentifierContext<? extends SchemaNode> getInstanceIdentifierContext() {
        return context;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }
}
