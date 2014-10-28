/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * DeleteModification store all the parameters required to delete a path from the data tree
 */
public class DeleteModification extends AbstractModification {
    private static final long serialVersionUID = 1L;

    public DeleteModification() {
    }

    public DeleteModification(YangInstanceIdentifier path) {
        super(path);
    }

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        transaction.delete(getPath());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        NormalizedNodeInputStreamReader streamReader = new NormalizedNodeInputStreamReader(in);
        setPath(streamReader.readYangInstanceIdentifier());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        NormalizedNodeOutputStreamWriter streamWriter = new NormalizedNodeOutputStreamWriter(out);
        streamWriter.writeYangInstanceIdentifier(getPath());
    }
}
