/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Deprecated
public class LegacyDOMRpcServiceAdapter extends ForwardingObject implements DOMRpcService {
    private final org.opendaylight.mdsal.dom.api.DOMRpcService delegate;

    public LegacyDOMRpcServiceAdapter(final org.opendaylight.mdsal.dom.api.DOMRpcService delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type,
                                                                  final NormalizedNode<?, ?> input) {
        final FluentFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult> future = delegate().invokeRpc(type, input);
        return future instanceof MdsalDOMRpcResultFutureAdapter ? ((MdsalDOMRpcResultFutureAdapter)future).delegate()
                : new LegacyDOMRpcResultFutureAdapter(future);
    }

    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(final T listener) {
        final ListenerRegistration<org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener> reg =
            delegate().registerRpcListener(new RpcAvailabilityListenerAdapter<>(listener));

        return new AbstractListenerRegistration<T>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        };
    }

    @Override
    protected org.opendaylight.mdsal.dom.api.DOMRpcService delegate() {
        return delegate;
    }
}
