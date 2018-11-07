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
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
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
    public FluentFuture<DOMRpcResult> invokeRpc(final SchemaPath type,
            final NormalizedNode<?, ?> input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(final T listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected DOMRpcService delegate() {
        return delegate;
    }
}
