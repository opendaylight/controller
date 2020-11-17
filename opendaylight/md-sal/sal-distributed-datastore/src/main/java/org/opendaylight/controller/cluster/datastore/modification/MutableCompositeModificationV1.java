/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.yang.data.api.schema.stream.ReusableStreamReceiver;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;

/**
 * Version of MutableCompositeModification with NNDO header written right after number of modifications
 * to create proper embedded block.
 */
public class MutableCompositeModificationV1 extends MutableCompositeModification {

    @Override
    protected void doReadExternal(ObjectInput in) throws IOException {
        final int size = in.readInt();
        if (size > 0) {
            final ReusableStreamReceiver receiver = ReusableImmutableNormalizedNodeStreamWriter.create();
            try {
                final NormalizedNodeDataInput input = NormalizedNodeDataInput.newDataInputWithoutValidation(in);

                for (int i = 0; i < size; i++) {
                    final byte type = input.readByte();
                    switch (type) {
                        case Modification.WRITE:
                            modifications.add(WriteModification.fromStream(input, getVersion(), receiver));
                            break;

                        case Modification.MERGE:
                            modifications.add(MergeModification.fromStream(input, getVersion(), receiver));
                            break;

                        case Modification.DELETE:
                            modifications.add(DeleteModification.fromStream(input, getVersion()));
                            break;
                        default:
                            break;
                    }
                }
            } finally {
                receiver.reset();
            }
        }
    }

    @Override
    protected void doWriteExternal(ObjectOutput out) throws IOException {
        final int size = modifications.size();
        out.writeInt(size);
        if (size > 0) {
            try (NormalizedNodeDataOutput stream = getStreamVersion().newDataOutput(out)) {
                for (Modification mod : modifications) {
                    stream.writeByte(mod.getType());
                    mod.writeTo(stream);
                }
            }
        }
    }
}
