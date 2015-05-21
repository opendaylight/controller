package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public class NormalizedNodeContext {

    private final InstanceIdentifierContext<? extends SchemaNode> context;
    private final NormalizedNode<?,?> data;
    private final boolean prettyPrint;

    public NormalizedNodeContext(final InstanceIdentifierContext<? extends SchemaNode> context,
                                 final NormalizedNode<?, ?> data, final boolean prettyPrint) {
        this.context = context;
        this.data = data;
        this.prettyPrint = prettyPrint;
    }

    public NormalizedNodeContext(final InstanceIdentifierContext<? extends SchemaNode> context,
                                 final NormalizedNode<?, ?> data) {
        this.context = context;
        this.data = data;
        this.prettyPrint = false;
    }

    public InstanceIdentifierContext<? extends SchemaNode> getInstanceIdentifierContext() {
        return context;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }


    public boolean isPrettyPrint() {
        return prettyPrint;
    }
}
