/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Utility class implementing {@link DefaultDOMRpcResult}.
 * @deprecated Use {@link org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult} instead.
 */
@Deprecated(forRemoval = true)
@Beta
public final class DefaultDOMRpcResult implements DOMRpcResult, Immutable, Serializable {
    private static final long serialVersionUID = 1L;

    // Flagged as "Non-transient non-serializable instance field" - the Collection is Serializable but the RpcError
    // interface isn't. In lieu of changing the interface, we assume the implementation is Serializable which is
    // reasonable since the only implementation that is actually used is from the RpcResultBuilder.
    @SuppressFBWarnings("SE_BAD_FIELD")
    private final Collection<? extends RpcError> errors;

    // Unfortunately the NormalizedNode interface isn't Serializable but we assume the implementations are.
    @SuppressFBWarnings("SE_BAD_FIELD")
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
        this(result, Collections.emptyList());
    }

    public DefaultDOMRpcResult(final NormalizedNode<?, ?> result,
            final @NonNull Collection<? extends RpcError> errors) {
        this.result = result;
        this.errors = requireNonNull(errors);
    }

    public DefaultDOMRpcResult(final @NonNull Collection<RpcError> errors) {
        this(null, errors);
    }

    @Override
    public Collection<? extends RpcError> getErrors() {
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
