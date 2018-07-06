/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Adapter for translating between {@link ClusteredDataTreeChangeListener} and
 * {@link org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener}.
 *
 * @author Thomas Pantelis
 */
final class BindingClusteredDOMDataTreeChangeListenerAdapter<T extends DataObject>
        extends BindingDataTreeChangeListenerAdapter<T>
        implements org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener<T> {
    BindingClusteredDOMDataTreeChangeListenerAdapter(ClusteredDataTreeChangeListener<T> listener) {
        super(listener);
    }
}
