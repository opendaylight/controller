/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.util.Set;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

final class RuntimeGeneratedInvoker implements NotificationInvoker {
    private final org.opendaylight.controller.sal.binding.api.NotificationListener<Notification> invocationProxy;
    private final RuntimeGeneratedInvokerPrototype prototype;
    private final NotificationListener delegate;

    @SuppressWarnings("unchecked")
    private RuntimeGeneratedInvoker(final NotificationListener delegate, final RuntimeGeneratedInvokerPrototype prototype, final org.opendaylight.controller.sal.binding.api.NotificationListener<?> proxy) {
        this.invocationProxy = (org.opendaylight.controller.sal.binding.api.NotificationListener<Notification>) proxy;
        this.delegate = Preconditions.checkNotNull(delegate);
        this.prototype = prototype;
    }

    public static RuntimeGeneratedInvoker create(final NotificationListener delegate, final RuntimeGeneratedInvokerPrototype prototype) throws InstantiationException, IllegalAccessException {
        final org.opendaylight.controller.sal.binding.api.NotificationListener<?> proxy = Preconditions.checkNotNull(prototype.getProtoClass().newInstance());
        RuntimeCodeHelper.setDelegate(proxy, delegate);
        return new RuntimeGeneratedInvoker(delegate, prototype, proxy);
    }

    @Override
    public NotificationListener getDelegate() {
        return delegate;
    }

    @Override
    public org.opendaylight.controller.sal.binding.api.NotificationListener<Notification> getInvocationProxy() {
        return invocationProxy;
    }

    @Override
    public Set<Class<? extends Notification>> getSupportedNotifications() {
        return prototype.getSupportedNotifications();
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + delegate.hashCode();
        result = prime * result + invocationProxy.hashCode();
        result = prime * result + prototype.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RuntimeGeneratedInvoker)) {
            return false;
        }
        final RuntimeGeneratedInvoker other = (RuntimeGeneratedInvoker) obj;
        if (!delegate.equals(other.delegate)) {
            return false;
        }
        if (!invocationProxy.equals(other.invocationProxy)) {
            return false;
        }
        return prototype.equals(other.prototype);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).toString();
    }
}
