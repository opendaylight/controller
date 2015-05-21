package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public class NormalizedNodeContext {

    private final InstanceIdentifierContext<? extends SchemaNode> context;
    private final NormalizedNode<?,?> data;
    private final WriterParameters writterParameters;

    public NormalizedNodeContext(final InstanceIdentifierContext<? extends SchemaNode> context,
                                 final NormalizedNode<?, ?> data, WriterParameters writterParameters) {
        this.context = context;
        this.data = data;
        this.writterParameters = writterParameters;
    }

    public NormalizedNodeContext(final InstanceIdentifierContext<? extends SchemaNode> context,
                                 final NormalizedNode<?, ?> data) {
        this.context = context;
        this.data = data;
        // default writter parameters
        this.writterParameters = new WriterParameters(false, Integer.MAX_VALUE);
    }

    public InstanceIdentifierContext<? extends SchemaNode> getInstanceIdentifierContext() {
        return context;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    public WriterParameters getWritterParameters() {
        return writterParameters;
    }
}
