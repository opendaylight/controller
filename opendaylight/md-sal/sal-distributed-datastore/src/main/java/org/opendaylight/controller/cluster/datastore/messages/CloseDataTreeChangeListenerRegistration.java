/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.registration.ListenerRegistrationMessages;

public final class CloseDataTreeChangeListenerRegistration implements SerializableMessage {
    public static final Class<ListenerRegistrationMessages.CloseDataTreeChangeListenerRegistration> SERIALIZABLE_CLASS =
            ListenerRegistrationMessages.CloseDataTreeChangeListenerRegistration.class;
    
    private static final CloseDataTreeChangeListenerRegistration SINGLETON = new CloseDataTreeChangeListenerRegistration();
    private final Object serializableForm;
    
    private CloseDataTreeChangeListenerRegistration() {
        serializableForm = ListenerRegistrationMessages.CloseDataTreeChangeListenerRegistration.newBuilder().build();
    }
    
    public static CloseDataTreeChangeListenerRegistration getInstance() {
        return SINGLETON;
    }

    @Override
    public Object toSerializable() {
        return serializableForm;
    }
}
