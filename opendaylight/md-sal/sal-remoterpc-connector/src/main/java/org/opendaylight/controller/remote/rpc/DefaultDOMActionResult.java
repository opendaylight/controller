package org.opendaylight.controller.remote.rpc;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

import java.io.Serializable;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Utility class implementing {@link org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult}.
 */
@Beta
@NonNullByDefault
public final class DefaultDOMActionResult implements DOMActionResult, Immutable {
    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Interfaces do not specify Serializable")
    private final @Nullable Optional<ContainerNode> result;
    private final Collection<RpcError> errors;

    public DefaultDOMActionResult(final Optional<ContainerNode> result, final RpcError... errors) {
        this(result, asCollection(errors));
    }

    public DefaultDOMActionResult(final RpcError... errors) {
        this(null, asCollection(errors));
    }

    public DefaultDOMActionResult(final @Nullable Optional<ContainerNode> result) {
        this(result, Collections.emptyList());
    }

    public DefaultDOMActionResult(final @Nullable Optional<ContainerNode> result,
                               final Collection<RpcError> errors) {
        this.result = result;
        this.errors = requireNonNull(errors);
    }

    public DefaultDOMActionResult(final Collection<RpcError> errors) {
        this(null, errors);
    }

    private static Collection<RpcError> asCollection(final RpcError... errors) {
        return errors.length == 0 ? Collections.emptyList() : Arrays.asList(errors);
    }

    @Override
    public Collection<RpcError> getErrors() {
        return errors;
    }

    @Override
    public @Nullable Optional<ContainerNode> getOutput() {
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
    public boolean equals(final @Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof org.opendaylight.controller.remote.rpc.DefaultDOMActionResult)) {
            return false;
        }
        final org.opendaylight.controller.remote.rpc.DefaultDOMActionResult other = (org.opendaylight.controller.remote.rpc.DefaultDOMActionResult) obj;
        return errors.equals(other.errors) && Objects.equals(result, other.result);
    }
}
