package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeContext {

    private final InstanceIdentifierContext context;
    private final NormalizedNode<?,?> data;

    /**
     * groupings are allowed for data desscritpion
     */
    private boolean groupingsAllowed;

    public NormalizedNodeContext(final InstanceIdentifierContext context, final NormalizedNode<?, ?> data, final boolean groupingsAllowed) {
        this(context, data);
        this.groupingsAllowed = groupingsAllowed;
    }

    public NormalizedNodeContext(InstanceIdentifierContext context, NormalizedNode<?, ?> data) {
        this.context = context;
        this.data = data;
        this.groupingsAllowed = false;
    }

    public InstanceIdentifierContext getInstanceIdentifierContext() {
        return context;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    public boolean isGroupingsAllowed() {
        return groupingsAllowed;
    }
}
