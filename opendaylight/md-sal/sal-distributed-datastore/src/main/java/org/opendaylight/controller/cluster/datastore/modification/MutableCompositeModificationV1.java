/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.List;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;

/**
 * Version of MutableCompositeModification with NNDO header written right after number of modifications
 * to create proper embedded block.
 */
public final class MutableCompositeModificationV1 extends MutableCompositeModification {

    @Override
    protected void doWriteExternal(final ObjectOutput out) throws IOException {
        List<Modification> modifications = getModifications();
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
