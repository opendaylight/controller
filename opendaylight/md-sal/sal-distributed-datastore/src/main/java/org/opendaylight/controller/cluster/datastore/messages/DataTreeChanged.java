/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public final class DataTreeChanged implements Externalizable {
    private static final long serialVersionUID = 1L;
    private final DataTreeCandidate change;
    
    public DataTreeChanged(DataTreeCandidate change) {
        this.change = Preconditions.checkNotNull(change);
    }

    public DataTreeCandidate getChange() {
        return change;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(change.getRootPath());
        
        // TODO Auto-generated method stub
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        final YangInstanceIdentifier treeId = (YangInstanceIdentifier) in.readObject();
        
        // TODO Auto-generated method stub
    }
}
