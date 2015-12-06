/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataInput;
import java.io.DataOutput;
import javax.annotation.Nonnull;

public final class NormalizedNodeInputOutput {
    private NormalizedNodeInputOutput() {
        throw new UnsupportedOperationException();
    }

    public static NormalizedNodeDataInput newDataInput(@Nonnull final DataInput input) {
        return new NormalizedNodeInputStreamReader(input);
    }

    public static NormalizedNodeDataOutput newDataOutput(@Nonnull final DataOutput output) {
        return new NormalizedNodeOutputStreamWriter(output);
    }
}
