/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * ClusteredDataTreeChangeListener is a marker interface to enable data tree change notifications on all
 * instances in a cluster where this listener is registered.
 * <p>
 * Applications should implement ClusteredDataTreeChangeListener instead of {@link DataTreeChangeListener},
 * if they want to listen for data tree change notifications on any node of a clustered data store.
 * {@link DataTreeChangeListener} enables notifications only at the leader of the data store.
 *
 * @author Thomas Pantelis
 *
 * @param <T> the DataObject type
 */
public interface ClusteredDataTreeChangeListener<T extends DataObject> extends DataTreeChangeListener<T> {

}
