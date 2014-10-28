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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

/**
 * WriteModification stores all the parameters required to write data to the specified path
 */
public class WriteModification extends AbstractModification {
    private static final long serialVersionUID = 1L;

    private transient NormalizedNode<?, ?> data;

    public WriteModification() {
    }

    public WriteModification(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        super(path);
        this.data = data;
    }

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        transaction.write(getPath(), data);
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        NormalizedNodeInputStreamReader streamReader = new NormalizedNodeInputStreamReader(in);
        data = streamReader.readNormalizedNode();
        setPath(streamReader.readYangInstanceIdentifier());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        NormalizedNodeOutputStreamWriter streamWriter = new NormalizedNodeOutputStreamWriter(out);
        NormalizedNodeWriter.forStreamWriter(streamWriter).write(getData());
        streamWriter.writeYangInstanceIdentifier(getPath());
    }
}
