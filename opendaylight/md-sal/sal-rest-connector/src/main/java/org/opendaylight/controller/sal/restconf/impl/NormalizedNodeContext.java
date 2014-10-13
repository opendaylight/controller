package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeContext {

    private final InstanceIdentifierContext context;
    private final NormalizedNode<?,?> data;
    private boolean prettyPrint = false;

    public NormalizedNodeContext(InstanceIdentifierContext context, NormalizedNode<?, ?> data) {
        this.context = context;
        this.data = data;
    }

    public NormalizedNodeContext(InstanceIdentifierContext context, NormalizedNode<?, ?> data, boolean prettyPrint) {
        this(context, data);
        this.prettyPrint = prettyPrint;
    }

    public InstanceIdentifierContext getInstanceIdentifierContext() {
        return context;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

}
