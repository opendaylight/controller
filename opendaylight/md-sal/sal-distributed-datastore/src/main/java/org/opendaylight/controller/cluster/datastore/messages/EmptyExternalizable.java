/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Externalizable with no data.
 *
 * @author Thomas Pantelis
 */
public class EmptyExternalizable implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 8413772905242947276L;

    @Override
    public void readExternal(ObjectInput in) {
    }

    @Override
    public void writeExternal(ObjectOutput out) {
    }
}
