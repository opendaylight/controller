/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

/**
 * Abstract base for notifying EntityOwnershipListeners.
 *
 * @author Thomas Pantelis
 */
abstract class EntityOwnershipChangePublisher {
    abstract void notifyEntityOwnershipListeners(DOMEntity entity, boolean wasOwner, boolean isOwner, boolean hasOwner);

    abstract String getLogId();
}
