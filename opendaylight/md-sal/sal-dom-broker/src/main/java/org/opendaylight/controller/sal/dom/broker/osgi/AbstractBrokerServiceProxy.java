/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.osgi;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.framework.ServiceReference;

public abstract class AbstractBrokerServiceProxy<T extends BrokerService> implements AutoCloseable, BrokerService {

    private T delegate;
    private final ServiceReference<T> reference;

    public AbstractBrokerServiceProxy(final @Nullable ServiceReference<T> ref, final T delegate) {
        this.delegate = checkNotNull(delegate, "Delegate should not be null.");
        this.reference = ref;
    }

    protected final T getDelegate() {
        checkState(delegate != null, "Proxy was closed and unregistered.");
        return delegate;
    }

    protected final ServiceReference<T> getReference() {
        return reference;
    }

    private final Set<Registration> registrations = Collections.synchronizedSet(new HashSet<Registration>());

    protected <R extends Registration> R addRegistration(final R registration) {
        if (registration != null) {
            registrations.add(registration);
        }
        return registration;
    }

    protected void closeBeforeUnregistrations() {
        // NOOP
    }

    protected void closeAfterUnregistrations() {
        // NOOP
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate = null;
            RuntimeException potentialException = new RuntimeException(
                    "Uncaught exceptions occured during unregistration");
            boolean hasSuppressed = false;
            for (Registration registration : registrations) {
                try {
                    registration.close();
                } catch (Exception e) {
                    potentialException.addSuppressed(e);
                    hasSuppressed = true;
                }
            }
            if (hasSuppressed) {
                throw potentialException;
            }
        }
    }
}
