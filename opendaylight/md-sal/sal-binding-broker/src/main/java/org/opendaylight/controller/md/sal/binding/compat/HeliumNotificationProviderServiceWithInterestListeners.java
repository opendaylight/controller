/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListener;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeliumNotificationProviderServiceWithInterestListeners extends HeliumNotificationProviderServiceAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(HeliumNotificationProviderServiceWithInterestListeners.class);

    private final ListenerRegistry<NotificationInterestListener> interestListeners = ListenerRegistry.create();
    private final ListenerRegistration<Listener> domListener;
    private final DOMNotificationService domService;
    private final BindingToNormalizedNodeCodec codec;

    public HeliumNotificationProviderServiceWithInterestListeners(
            final BindingDOMNotificationPublishServiceAdapter publishService, final BindingDOMNotificationServiceAdapter listenService, final DOMNotificationSubscriptionListenerRegistry registry) {
        super(publishService, listenService);
        this.codec = publishService.getCodecRegistry();
        this.domListener = registry.registerSubscriptionListener(new Listener());
        this.domService = listenService.getDomService();
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(
            final NotificationInterestListener listener) {
        notifyListener(listener, translate(domListener.getInstance().getAllObserved()));
        return interestListeners.register(listener);
    }

    private Set<Class<? extends Notification>> translate(final Set<SchemaPath> added) {
        return codec.getNotificationClasses(added);
    }

    private void notifyAllListeners(final Set<SchemaPath> added) {
        final Iterator<ListenerRegistration<NotificationInterestListener>> listeners = interestListeners.iterator();
        if(listeners.hasNext()) {
            final Set<Class<? extends Notification>> baEvent = translate(added);
            while(listeners.hasNext()) {
                final NotificationInterestListener listenerRef = listeners.next().getInstance();
                try {
                    notifyListener(listenerRef,baEvent);
                } catch (final Exception e) {
                    LOG.warn("Unhandled exception during invoking listener {}",e, listenerRef);
                }
            }
        }
    }

    @Override
    public <T extends Notification> ListenerRegistration<NotificationListener<T>> registerNotificationListener(
            final Class<T> type, final NotificationListener<T> listener) {

        final FunctionalNotificationListenerAdapter<T> adapter = new FunctionalNotificationListenerAdapter<>(codec, type, listener);
        final SchemaPath domType = SchemaPath.create(true, BindingReflections.findQName(type));
        final ListenerRegistration<?> domReg = domService.registerNotificationListener(adapter, domType);
        return new AbstractListenerRegistration<NotificationListener<T>>(listener) {
            @Override
            protected void removeRegistration() {
                domReg.close();
            }

        };
    }

    private void notifyListener(final NotificationInterestListener listener, final Set<Class<? extends Notification>> baEvent) {
        for(final Class<? extends Notification> event: baEvent) {
            listener.onNotificationSubscribtion(event);
        }
    }

    private final class Listener implements DOMNotificationSubscriptionListener {

        private volatile Set<SchemaPath> allObserved = Collections.emptySet();

        @Override
        public void onSubscriptionChanged(final Set<SchemaPath> currentTypes) {
            final Set<SchemaPath> added = Sets.difference(currentTypes, allObserved).immutableCopy();
            notifyAllListeners(added);
            allObserved = Sets.union(allObserved, added).immutableCopy();
        }

        Set<SchemaPath> getAllObserved() {
            return allObserved;
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        domListener.close();
    }
}
