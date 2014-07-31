/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;

public class ApplySnapshot implements Serializable {
    private final Object snapshot;

    public ApplySnapshot(Object snapshot) {
        this.snapshot = snapshot;
    }

    public Object getSnapshot() {
        return snapshot;
    }
}
