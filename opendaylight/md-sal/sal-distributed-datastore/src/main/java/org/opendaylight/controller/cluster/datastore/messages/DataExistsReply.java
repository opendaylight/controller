/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;

public class DataExistsReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean exists;

    public DataExistsReply(boolean exists) {
        this.exists = exists;
    }

    public boolean exists() {
        return exists;
    }
}
