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
import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Deprecated
public class DOMRpcServiceAdapter extends ForwardingObject implements org.opendaylight.mdsal.dom.api.DOMRpcService {
    private final DOMRpcService delegate;

    public DOMRpcServiceAdapter(final DOMRpcService delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public FluentFuture<DOMRpcResult> invokeRpc(final SchemaPath type, final NormalizedNode<?, ?> input) {
        return new MdsalDOMRpcResultFutureAdapter(delegate().invokeRpc(type, input));
    }

    @Override
    public <T extends org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener> ListenerRegistration<T>
            registerRpcListener(final T listener) {
        final ListenerRegistration<?> reg = delegate().registerRpcListener(new DOMRpcAvailabilityListener() {
            @Override
            public void onRpcAvailable(final Collection<DOMRpcIdentifier> rpcs) {
                listener.onRpcAvailable(convert(rpcs));
            }

            @Override
            public void onRpcUnavailable(final Collection<DOMRpcIdentifier> rpcs) {
                listener.onRpcUnavailable(convert(rpcs));
            }
        });

        return new AbstractListenerRegistration<T>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        };
    }

    @Override
    protected DOMRpcService delegate() {
        return delegate;
    }

    public static Set<org.opendaylight.mdsal.dom.api.DOMRpcIdentifier> convert(
            final Collection<DOMRpcIdentifier> rpcs) {
        return rpcs.stream().map(DOMRpcIdentifier::toMdsal).collect(Collectors.toSet());
    }
}
