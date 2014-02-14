/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Path;

@SuppressWarnings("all")
class DataChangeListenerRegistration<P extends Path<P>, D extends Object, DCL extends DataChangeListener<P, D>> extends
        AbstractObjectRegistration<DCL> implements ListenerRegistration<DCL> {
    private AbstractDataBroker<P, D, DCL> dataBroker;

    private final P path;

    public P getPath() {
        return this.path;
    }

    public DataChangeListenerRegistration(final P path, final DCL instance, final AbstractDataBroker<P, D, DCL> broker) {
        super(instance);
        this.dataBroker = broker;
        this.path = path;
    }

    @Override
    protected void removeRegistration() {
        this.dataBroker.removeListener(this);
        this.dataBroker = null;
    }
}
