/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.impl.BindingAdapterBuilder.Factory;
import org.opendaylight.mdsal.binding.api.BindingService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

public class BindingNotificationServiceAdapter implements NotificationService {

    public static final Factory<NotificationService> BUILDER_FACTORY = Builder::new;

    private final org.opendaylight.mdsal.binding.api.NotificationService delegate;

    public BindingNotificationServiceAdapter(final org.opendaylight.mdsal.binding.api.NotificationService delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public <T extends NotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener) {
        return delegate.registerNotificationListener(listener);
    }

    private static class Builder extends BindingAdapterBuilder<NotificationService> {
        @Override
        public Set<? extends Class<? extends BindingService>> getRequiredDelegates() {
            return ImmutableSet.of(org.opendaylight.mdsal.binding.api.NotificationService.class);
        }

        @Override
        protected NotificationService createInstance(ClassToInstanceMap<BindingService> delegates) {
            return new BindingNotificationServiceAdapter(delegates.getInstance(
                    org.opendaylight.mdsal.binding.api.NotificationService.class));
        }
    }
}
