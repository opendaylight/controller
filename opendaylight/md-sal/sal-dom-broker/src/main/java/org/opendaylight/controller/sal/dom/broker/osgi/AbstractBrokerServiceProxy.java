package org.opendaylight.controller.sal.dom.broker.osgi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.framework.ServiceReference;
import static com.google.common.base.Preconditions.*;

public abstract class AbstractBrokerServiceProxy<T extends BrokerService> implements AutoCloseable, BrokerService {

    private T delegate;
    private final ServiceReference<T> reference;

    public AbstractBrokerServiceProxy(ServiceReference<T> ref, T delegate) {
        this.delegate = checkNotNull(delegate, "Delegate should not be null.");
        this.reference = checkNotNull(ref, "Reference should not be null.");
    }

    protected final T getDelegate() {
        checkState(delegate != null, "Proxy was closed and unregistered.");
        return delegate;
    }

    protected final ServiceReference<T> getReference() {
        return reference;
    }

    private Set<Registration<?>> registrations = Collections.synchronizedSet(new HashSet<Registration<?>>());

    protected <R extends Registration<?>> R addRegistration(R registration) {
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
            for (Registration<?> registration : registrations) {
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
