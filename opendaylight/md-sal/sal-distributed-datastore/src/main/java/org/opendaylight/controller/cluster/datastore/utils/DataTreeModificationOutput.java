/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Throwables;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
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

    public static void toFile(File file, DataTreeModification modification) {
        try(FileOutputStream outStream = new FileOutputStream(file)) {
            modification.applyToCursor(new DataTreeModificationOutputCursor(new DataOutputStream(outStream)));
        } catch(Exception e) {
            LOG.error("Error writing DataTreeModification to file {}", file, e);
        }
    }

    private static class DataTreeModificationOutputCursor extends AbstractDataTreeModificationCursor {
        private final DataOutputStream output;

        DataTreeModificationOutputCursor(DataOutputStream output) {
            this.output = output;
        }

        @Override
        public void delete(PathArgument child) {
            try {
                output.write("\nDELETE -> ".getBytes());
                output.write(next(child).toString().getBytes());
                output.writeByte('\n');
            } catch(IOException e) {
                Throwables.propagate(e);
            }
        }

        @Override
        public void merge(PathArgument child, NormalizedNode<?, ?> data) {
            outputPathAndNode("MERGE", child, data);
        }

        @Override
        public void write(PathArgument child, NormalizedNode<?, ?> data) {
            outputPathAndNode("WRITE", child, data);
        }

        private void outputPathAndNode(String name, PathArgument child, NormalizedNode<?, ?> data) {
            try {
                output.writeByte('\n');
                output.write(name.getBytes());
                output.write(" -> ".getBytes());
                output.write(next(child).toString().getBytes());
                output.write(": \n".getBytes());
                NormalizedNodeXMLOutput.toStream(output, data);
                output.writeByte('\n');
            } catch(IOException | XMLStreamException e) {
                Throwables.propagate(e);
            }
        }
    }
}
