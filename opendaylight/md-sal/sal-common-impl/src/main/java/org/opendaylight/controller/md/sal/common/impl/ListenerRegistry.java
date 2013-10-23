package org.opendaylight.controller.md.sal.common.impl;

import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.*;

import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public class ListenerRegistry<T extends EventListener> {

    final Set<ListenerRegistration<T>> listeners;
    final Set<ListenerRegistration<T>> unmodifiableView;

    public ListenerRegistry() {
        listeners = new HashSet<>();
        unmodifiableView = Collections.unmodifiableSet(listeners);
    }

    public Iterable<ListenerRegistration<T>> getListeners() {
        return unmodifiableView;
    }

    
    public ListenerRegistration<T> register(T listener) {
        checkNotNull(listener, "Listener should not be null.");
        ListenerRegistrationImpl<T> ret = new ListenerRegistrationImpl<T>(listener);
        listeners.add(ret);
        return ret;
    }
    
    
    @SuppressWarnings("rawtypes")
    private void remove(ListenerRegistrationImpl registration) {
        listeners.remove(registration);
    }

    private class ListenerRegistrationImpl<P extends EventListener> //
            extends AbstractObjectRegistration<P> //
            implements ListenerRegistration<P> {

        public ListenerRegistrationImpl(P instance) {
            super(instance);
        }

        @Override
        protected void removeRegistration() {
            ListenerRegistry.this.remove(this);
        }
    }
}
