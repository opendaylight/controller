/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.IOException;
import java.io.Serializable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

public class ReadDataReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient NormalizedNode<?, ?> normalizedNode;

    public ReadDataReply(NormalizedNode<?, ?> normalizedNode){

        this.normalizedNode = normalizedNode;
    }

    public NormalizedNode<?, ?> getNormalizedNode() {
        return normalizedNode;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if(normalizedNode != null) {
            out.writeBoolean(true);
            NormalizedNodeOutputStreamWriter streamWriter = new NormalizedNodeOutputStreamWriter(out);
            NormalizedNodeWriter.forStreamWriter(streamWriter).write(normalizedNode);
        } else {
            out.writeBoolean(false);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        boolean present = in.readBoolean();
        if(present) {
            @SuppressWarnings("resource")
            NormalizedNodeInputStreamReader streamReader = new NormalizedNodeInputStreamReader(in);
            normalizedNode = streamReader.readNormalizedNode();
        }
    }
}
