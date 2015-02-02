package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.CheckedFuture;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListenerRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Utility {@link DOMRpcService} which forwards all requests to a backing delegate instance.
 */
public abstract class ForwardingDOMRpcService extends ForwardingObject implements DOMRpcService {
    @Override
    protected abstract @Nonnull DOMRpcService delegate();

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type, final NormalizedNode<?, ?> input) {
        return delegate().invokeRpc(type, input);
    }

    @Override
    public DOMRpcAvailabilityListenerRegistration registerRpcListener(final DOMRpcAvailabilityListener listener) {
        return delegate().registerRpcListener(listener);
    }
}
