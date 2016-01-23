/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class RegisterChangeListener implements ListenerRegistrationMessage {
    private final YangInstanceIdentifier path;
    private final ActorRef dataChangeListener;
    private final AsyncDataBroker.DataChangeScope scope;
    private final boolean registerOnAllInstances;

    public RegisterChangeListener(YangInstanceIdentifier path,
        ActorRef dataChangeListener,
        AsyncDataBroker.DataChangeScope scope, boolean registerOnAllInstances) {
        this.path = path;
        this.dataChangeListener = dataChangeListener;
        this.scope = scope;
        this.registerOnAllInstances = registerOnAllInstances;
    }

    @Override
    public YangInstanceIdentifier getPath() {
        return path;
    }

    public AsyncDataBroker.DataChangeScope getScope() {
        return scope;
    }

    public ActorPath getDataChangeListenerPath() {
        return dataChangeListener.path();
    }

    @Override
    public boolean isRegisterOnAllInstances() {
        return registerOnAllInstances;
    }
}
