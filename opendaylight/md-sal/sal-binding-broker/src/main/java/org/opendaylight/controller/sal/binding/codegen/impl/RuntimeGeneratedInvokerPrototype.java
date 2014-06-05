/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import java.util.Set;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

final class RuntimeGeneratedInvokerPrototype {
    private final Set<Class<? extends Notification>> supportedNotifications;
    private final Class<? extends NotificationListener<?>> protoClass;

    public RuntimeGeneratedInvokerPrototype(final Set<Class<? extends Notification>> supportedNotifications, final Class<? extends NotificationListener<?>> protoClass) {
        this.supportedNotifications = Preconditions.checkNotNull(supportedNotifications);
        this.protoClass = Preconditions.checkNotNull(protoClass);
    }

    public Set<Class<? extends Notification>> getSupportedNotifications() {
        return supportedNotifications;
    }

    public Class<? extends NotificationListener<?>> getProtoClass() {
        return protoClass;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + supportedNotifications.hashCode();
        result = prime * result + protoClass.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RuntimeGeneratedInvokerPrototype)) {
            return false;
        }
        final RuntimeGeneratedInvokerPrototype other = (RuntimeGeneratedInvokerPrototype) obj;
        if (!protoClass.equals(other.protoClass)) {
            return false;
        }
        return supportedNotifications.equals(other.supportedNotifications);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("protoClass", protoClass)
                .add("supportedNotifications", supportedNotifications)
                .toString();
    }
}
