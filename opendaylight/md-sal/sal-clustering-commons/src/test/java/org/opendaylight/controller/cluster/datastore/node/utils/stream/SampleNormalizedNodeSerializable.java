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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class SampleNormalizedNodeSerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    private NormalizedNode<?, ?> input;

    public SampleNormalizedNodeSerializable(final NormalizedNode<?, ?> input) {
        this.input = input;
    }

    public NormalizedNode<?, ?> getInput() {
        return input;
    }

    private void readObject(final ObjectInputStream stream) throws IOException {
        this.input = NormalizedNodeInputOutput.newDataInput(stream).readNormalizedNode();
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
        NormalizedNodeInputOutput.newDataOutput(stream).writeNormalizedNode(input);
    }

}
