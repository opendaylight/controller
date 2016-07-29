/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.opendaylight.controller.cluster.datastore.utils.NormalizedNodeXMLOutput;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

abstract class AbstractShardDataTreeSnapshot {
    /**
     * Legacy data tree snapshot, which contains only the data.
     */
    private static final class Legacy extends AbstractShardDataTreeSnapshot {
        private final NormalizedNode<?, ?> rootNode;
        
        
    }
    
    private static final byte[] TYPED_SIGNATURE = "TYPED".getBytes(StandardCharsets.US_ASCII);
    
    private AbstractShardDataTreeSnapshot() {
        throw new UnsupportedOperationException();
    }

    /**
     * Version prior to Boron did not encapsulate the type being persisted. To maintain compatibility, we introduce
     * header in the payload.
     */
    private static boolean isTypedSnapshot(final byte[] snapshotBytes) {
        if (snapshotBytes.length < TYPED_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < TYPED_SIGNATURE.length; ++i) {
            if (snapshotBytes[i] != TYPED_SIGNATURE[i]) {
                return false;
            }
        }
        
        return true;
    }

    static AbstractShardDataTreeSnapshot deserialize(final byte[] snapshotBytes) {
        if (!isTypedSnapshot(snapshotBytes)) {
            final NormalizedNode<?, ?> node = SerializationUtils.deserializeNormalizedNode(snapshotBytes);
            final PruningDataTreeModification tx = new PruningDataTreeModification(store.newModification(),
                    store.getDataTree(), schemaContext);
            tx.write(YangInstanceIdentifier.EMPTY, node);
            try {
                commitTransaction(tx);
            } catch (Exception e) {
                File file = new File(System.getProperty("karaf.data", "."),
                        "failed-recovery-snapshot-" + shardName + ".xml");
                NormalizedNodeXMLOutput.toFile(file, node);
                throw new RuntimeException(String.format(
                        "%s: Failed to apply recovery snapshot. Node data was written to file %s",
                        shardName, file), e);
            }
            
            
        }
        
        
    }
}

