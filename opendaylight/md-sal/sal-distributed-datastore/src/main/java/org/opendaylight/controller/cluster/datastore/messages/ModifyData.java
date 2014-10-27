/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

public abstract class ModifyData implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient YangInstanceIdentifier path;
    private transient NormalizedNode<?, ?> data;

    public ModifyData() {
    }

    public ModifyData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        this.path = path;
        this.data = data;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        NormalizedNodeInputStreamReader streamReader = new NormalizedNodeInputStreamReader(in);
        data = streamReader.readNormalizedNode();
        path = streamReader.readYangInstanceIdentifier();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        NormalizedNodeOutputStreamWriter streamWriter = new NormalizedNodeOutputStreamWriter(out);
        NormalizedNodeWriter.forStreamWriter(streamWriter).write(getData());
        streamWriter.writeYangInstanceIdentifier(getPath());
    }
}
