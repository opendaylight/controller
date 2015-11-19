/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Adapter wrapping Binding {@link ClusteredDataTreeChangeListener} and exposing
 * it as {@link ClusteredDOMDataTreeChangeListener} and translated DOM events
 * to their Binding equivalent.
 *
 * @author Thomas Pantelis
 */
final class BindingClusteredDOMDataTreeChangeListenerAdapter<T extends DataObject>
        extends BindingDOMDataTreeChangeListenerAdapter<T> implements ClusteredDOMDataTreeChangeListener {
    BindingClusteredDOMDataTreeChangeListenerAdapter(BindingToNormalizedNodeCodec codec,
            ClusteredDataTreeChangeListener<T> listener, LogicalDatastoreType store) {
        super(codec, listener, store);
    }
}
