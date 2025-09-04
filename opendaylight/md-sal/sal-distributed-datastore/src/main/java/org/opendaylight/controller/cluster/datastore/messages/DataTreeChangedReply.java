/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public final class DataTreeChangedReply implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public static final DataTreeChangedReply INSTANCE = new DataTreeChangedReply();

    private DataTreeChangedReply() {
        // Hiddeon in purpose
    }

    @java.io.Serial
    private Object readResolve() {
        return INSTANCE;
    }
}
