/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLStreamException;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to output DataTreeModifications in readable format.
 *
 * @author Thomas Pantelis
 */
public final class DataTreeModificationOutput {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeModificationOutput.class);

    private DataTreeModificationOutput() {
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void toFile(final File file, final DataTreeModification modification) {
        try (FileOutputStream outStream = new FileOutputStream(file)) {
            modification.applyToCursor(new DataTreeModificationOutputCursor(new DataOutputStream(outStream)));
        } catch (IOException | RuntimeException e) {
            LOG.error("Error writing DataTreeModification to file {}", file, e);
        }
    }

    private static class DataTreeModificationOutputCursor extends AbstractDataTreeModificationCursor {
        private final DataOutputStream output;

        DataTreeModificationOutputCursor(final DataOutputStream output) {
            this.output = output;
        }

        @Override
        public void delete(final PathArgument child) {
            try {
                output.write("\nDELETE -> ".getBytes(StandardCharsets.UTF_8));
                output.write(current().node(child).toString().getBytes(StandardCharsets.UTF_8));
                output.writeByte('\n');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void merge(final PathArgument child, final NormalizedNode data) {
            outputPathAndNode("MERGE", child, data);
        }

        @Override
        public void write(final PathArgument child, final NormalizedNode data) {
            outputPathAndNode("WRITE", child, data);
        }

        private void outputPathAndNode(final String name, final PathArgument child, final NormalizedNode data) {
            try {
                output.writeByte('\n');
                output.write(name.getBytes(StandardCharsets.UTF_8));
                output.write(" -> ".getBytes(StandardCharsets.UTF_8));
                output.write(current().node(child).toString().getBytes(StandardCharsets.UTF_8));
                output.write(": \n".getBytes(StandardCharsets.UTF_8));
                NormalizedNodeXMLOutput.toStream(output, data);
                output.writeByte('\n');
            } catch (IOException | XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
