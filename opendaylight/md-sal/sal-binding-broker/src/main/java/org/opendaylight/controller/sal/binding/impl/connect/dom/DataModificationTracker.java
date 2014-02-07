/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.concepts.Path;

public final class DataModificationTracker<P extends Path<P>,D> {
    ConcurrentMap<Object, DataModification<P,D>> trackedTransactions = new ConcurrentHashMap<>();

    public void startTrackingModification(DataModification<P,D> modification) {
        trackedTransactions.putIfAbsent(modification.getIdentifier(), modification);
    }

    public boolean containsIdentifier(Object identifier) {
        return trackedTransactions.containsKey(identifier);
    }
}
