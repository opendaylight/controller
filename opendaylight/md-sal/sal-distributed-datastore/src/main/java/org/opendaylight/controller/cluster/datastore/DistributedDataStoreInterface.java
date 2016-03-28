/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;

/**
 * The public interface exposed vi a DistributedDataStore via the OSGi registry.
 *
 * @author Thomas Pantelis
 */
public interface DistributedDataStoreInterface extends DOMStore {

    ActorContext getActorContext();
}