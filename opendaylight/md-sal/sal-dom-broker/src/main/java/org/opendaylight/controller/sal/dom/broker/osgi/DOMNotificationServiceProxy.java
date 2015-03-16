/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.osgi.framework.ServiceReference;

import javax.annotation.Nonnull;
import java.util.Collection;

public class DOMNotificationServiceProxy extends AbstractBrokerServiceProxy<DOMNotificationService> implements
        DOMNotificationService {

    public DOMNotificationServiceProxy(ServiceReference<DOMNotificationService> ref, DOMNotificationService delegate) {
        super(ref, delegate);
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(@Nonnull final T listener, @Nonnull final Collection<SchemaPath> types) {
        return addRegistration(getDelegate().registerNotificationListener(listener, types));
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(@Nonnull final T listener, final SchemaPath... types) {
        return addRegistration(getDelegate().registerNotificationListener(listener, types));
    }
}
