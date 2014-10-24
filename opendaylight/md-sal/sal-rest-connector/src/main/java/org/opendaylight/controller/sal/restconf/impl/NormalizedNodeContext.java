package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeContext {

    private final InstanceIdentifierContext context;
    private final NormalizedNode<?,?> data;
    private Double depth = Double.MAX_VALUE;

    public NormalizedNodeContext(InstanceIdentifierContext context, NormalizedNode<?, ?> data, final Double depth) {
        this(context,data);
        this.depth = depth;
    }

    public NormalizedNodeContext(InstanceIdentifierContext context, NormalizedNode<?, ?> data) {
        this.context = context;
        this.data = data;
    }

    public InstanceIdentifierContext getInstanceIdentifierContext() {
        return context;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    public Double getDepth() {
        return depth;
    }
}
