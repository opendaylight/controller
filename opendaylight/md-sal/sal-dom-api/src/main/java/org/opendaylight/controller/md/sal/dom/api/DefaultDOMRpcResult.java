/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Utility class implementing {@link DefaultDOMRpcResult}.
 */
public class DefaultDOMRpcResult implements DOMRpcResult {
    private final Collection<RpcError> errors;
    private final NormalizedNode<?, ?> result;

    public DefaultDOMRpcResult(final NormalizedNode<?, ?> result, final Collection<RpcError> errors) {
        this.result = result;
        this.errors = Preconditions.checkNotNull(errors);
    }

    public DefaultDOMRpcResult(final NormalizedNode<?, ?> result) {
        this(result, Collections.<RpcError>emptyList());
    }

    public DefaultDOMRpcResult(final Collection<RpcError> errors) {
        this(null, errors);
    }

    @Override
    public Collection<RpcError> getErrors() {
        return errors;
    }

    @Override
    public NormalizedNode<?, ?> getResult() {
        return result;
    }
}
