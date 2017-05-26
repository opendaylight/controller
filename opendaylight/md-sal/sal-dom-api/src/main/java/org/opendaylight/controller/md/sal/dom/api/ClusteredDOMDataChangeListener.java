/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.api;

/**
 * <p>ClusteredDOMDataChangeListener is a marker interface to enable data change notifications on all instances in a cluster,
 * where this listener is registered.</p>
 *
 *
 * <p>Applications should implement ClusteredDOMDataChangeListener instead of DOMDataChangeListener, if they want to listen
 * to data change notifications on any node of clustered datastore. DOMDataChangeListener enables data change notifications
 * only at leader of the datastore shard.</p>
 *
 */

public interface ClusteredDOMDataChangeListener extends DOMDataChangeListener{

}
