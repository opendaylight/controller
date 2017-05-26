/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

class BindingDataTreeChangeListenerRegistration<L extends DataTreeChangeListener<?>> extends AbstractListenerRegistration<L> {

    private final ListenerRegistration<?> domReg;

    BindingDataTreeChangeListenerRegistration(final L listener, final ListenerRegistration<?> domReg) {
        super(listener);
        this.domReg = Preconditions.checkNotNull(domReg);
    }

    @Override
    protected void removeRegistration() {
        domReg.close();
    }
}
