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

final class SodiumSR1DataOutput extends AbstractMagnesiumDataOutput {
    SodiumSR1DataOutput(final DataOutput output) {
        super(output);
    }

    @Override
    short streamVersion() {
        return TokenTypes.SODIUM_SR1_VERSION;
    }

    @Override
    void writeValue(final BigInteger value) throws IOException {
        output.writeByte(MagnesiumValue.BIGINTEGER);
        output.writeUTF(value.toString());
    }
}
