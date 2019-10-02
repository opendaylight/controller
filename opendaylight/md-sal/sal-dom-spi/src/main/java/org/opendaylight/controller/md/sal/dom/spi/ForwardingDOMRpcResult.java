/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.collect.ForwardingObject;
import java.util.Collection;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Utility class which implements {@link DOMRpcResult} by forwarding all methods
 * to a backing instance.
 *
 * @deprecated Use {@link org.opendaylight.mdsal.dom.spi.ForwardingDOMRpcResult} instead.
 */
@Deprecated(forRemoval = true)
public abstract class ForwardingDOMRpcResult extends ForwardingObject implements DOMRpcResult {
    @Override
    protected abstract @NonNull DOMRpcResult delegate();

    @Override
    public Collection<? extends RpcError> getErrors() {
        return delegate().getErrors();
    }

    @Override
    public NormalizedNode<?, ?> getResult() {
        return delegate().getResult();
    }
}
