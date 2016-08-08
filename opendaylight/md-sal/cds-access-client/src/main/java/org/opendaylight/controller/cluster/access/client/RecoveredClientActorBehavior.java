/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

/**
 * @param <C> Concrete context type
 *
 * @author Robert Varga
 */
abstract class RecoveredClientActorBehavior<C extends AbstractClientActorContext> extends AbstractClientActorBehavior<C> {

    RecoveredClientActorBehavior(final C context) {
        super(context);
    }

    @Override
    final AbstractClientActorBehavior<?> onReceiveRecover(Object recover) {
        throw new IllegalStateException("Frontend has been recovered");
    }
}
