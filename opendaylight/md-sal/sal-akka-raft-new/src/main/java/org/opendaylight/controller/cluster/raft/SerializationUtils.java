/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;

public class SerializationUtils {

    public static Object fromSerializable(Object serializable){
        if (InstallSnapshot.isSerializedType(serializable)) {
            return InstallSnapshot.fromSerializable(serializable);
        }
        return serializable;
    }
}
