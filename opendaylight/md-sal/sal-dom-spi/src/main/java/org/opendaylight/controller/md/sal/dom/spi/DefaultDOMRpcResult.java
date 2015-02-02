/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Utility class implementing {@link DefaultDOMRpcResult}.
 */
@Beta
public final class DefaultDOMRpcResult implements DOMRpcResult {
    private final Collection<RpcError> errors;
    private final NormalizedNode<?, ?> result;

    private static Collection<RpcError> asCollection(final RpcError... errors) {
        if (errors.length == 0) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(errors);
        }
    }

    public DefaultDOMRpcResult(final NormalizedNode<?, ?> result, final RpcError... errors) {
        this(result, asCollection(errors));
    }

    public DefaultDOMRpcResult(final RpcError... errors) {
        this(null, asCollection(errors));
    }

    public DefaultDOMRpcResult(final NormalizedNode<?, ?> result) {
        this(result, Collections.<RpcError>emptyList());
    }

    public DefaultDOMRpcResult(final NormalizedNode<?, ?> result, final @Nonnull Collection<RpcError> errors) {
        this.result = result;
        this.errors = Preconditions.checkNotNull(errors);
    }

    public DefaultDOMRpcResult(final @Nonnull Collection<RpcError> errors) {
        this(null, errors);
    }

    @Override
    public @Nonnull Collection<RpcError> getErrors() {
        return errors;
    }

    @Override
    public NormalizedNode<?, ?> getResult() {
        return result;
    }

    @Override
    public int hashCode() {
        int ret = errors.hashCode();
        if (result != null) {
            ret = 31 * ret + result.hashCode();
        }
        return ret;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DefaultDOMRpcResult)) {
            return false;
        }

        final DefaultDOMRpcResult other = (DefaultDOMRpcResult) obj;
        if (!errors.equals(other.errors)) {
            return false;
        }
        return Objects.equals(result, other.result);
    }
}
