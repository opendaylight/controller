/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.base.Preconditions;

public abstract class AbstractClientActorBehavior<C extends AbstractClientActorContext> {
    private final C context;

    AbstractClientActorBehavior(final C context) {
        this.context = Preconditions.checkNotNull(context);
    }

    protected final C context() {
        return context;
    }

    protected final String persistenceId() {
        return context.persistenceId();
    }

    abstract AbstractClientActorBehavior<?> onReceiveCommand(Object command);
    abstract AbstractClientActorBehavior<?> onReceiveRecover(Object recover);
}
