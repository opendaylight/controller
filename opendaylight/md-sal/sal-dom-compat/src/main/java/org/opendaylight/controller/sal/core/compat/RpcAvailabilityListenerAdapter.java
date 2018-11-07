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
import java.util.Collection;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;

@Deprecated
public class RpcAvailabilityListenerAdapter<T extends DOMRpcAvailabilityListener> extends ForwardingObject
        implements org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener {
    private final @NonNull T delegate;

    public RpcAvailabilityListenerAdapter(final T delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public void onRpcAvailable(final Collection<org.opendaylight.mdsal.dom.api.DOMRpcIdentifier> rpcs) {
        delegate.onRpcAvailable(convert(rpcs));
    }

    @Override
    public void onRpcUnavailable(final Collection<org.opendaylight.mdsal.dom.api.DOMRpcIdentifier> rpcs) {
        delegate.onRpcUnavailable(convert(rpcs));
    }

    @Override
    protected T delegate() {
        return delegate;
    }

    private static @NonNull Collection<DOMRpcIdentifier> convert(
            final Collection<org.opendaylight.mdsal.dom.api.DOMRpcIdentifier> from) {
        return from.stream().map(DOMRpcIdentifier::fromMdsal).collect(Collectors.toList());
    }
}
