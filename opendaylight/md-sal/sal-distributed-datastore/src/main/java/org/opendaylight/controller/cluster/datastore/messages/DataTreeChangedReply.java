/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.ObjectStreamException;
import java.io.Serializable;

public final class DataTreeChangedReply implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DataTreeChangedReply INSTANCE = new DataTreeChangedReply();

    private DataTreeChangedReply() {
        // Use getInstance() instead
    }

    public static DataTreeChangedReply getInstance() {
        return INSTANCE;
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
