/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import java.io.Serializable;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * An abstract base class for invocation responses. Specialized via {@link ActionResponse} and {@link RpcResponse}.
 */
public abstract class AbstractResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final transient @Nullable ContainerNode output;

    public AbstractResponse(final @Nullable ContainerNode output) {
        this.output = output;
    }

    public final @Nullable ContainerNode getOutput() {
        return output;
    }

    abstract Object writeReplace();
}
