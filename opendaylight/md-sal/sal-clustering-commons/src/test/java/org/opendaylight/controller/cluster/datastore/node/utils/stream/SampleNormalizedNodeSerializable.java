/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

public class SampleNormalizedNodeSerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    private NormalizedNode<?, ?> input;

    public SampleNormalizedNodeSerializable(NormalizedNode<?, ?> input) {
        this.input = input;
    }

    public NormalizedNode<?, ?> getInput() {
        return input;
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException, URISyntaxException {
        NormalizedNodeDataInput reader = new NormalizedNodeInputStreamReader(stream);
        this.input = reader.readNormalizedNode();
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
        NormalizedNodeStreamWriter writer = new NormalizedNodeOutputStreamWriter(stream);
        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(writer);

        normalizedNodeWriter.write(this.input);
    }

}
