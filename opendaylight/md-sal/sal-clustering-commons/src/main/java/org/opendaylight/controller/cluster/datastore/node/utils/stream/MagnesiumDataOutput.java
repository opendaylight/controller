/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;

final class MagnesiumDataOutput extends AbstractMagnesiumDataOutput {
    MagnesiumDataOutput(final DataOutput output) {
        super(output);
    }

    @Override
    short streamVersion() {
        return TokenTypes.MAGNESIUM_VERSION;
    }

    @Override
    void writeValue(final BigInteger value) throws IOException {
        throw new IOException("BigInteger values are not supported");
    }
}
