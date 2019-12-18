/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to output NormalizedNodes as XML.
 *
 * @author Thomas Pantelis
 */
public final class NormalizedNodeXMLOutput {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodeXMLOutput.class);
    private static final XMLOutputFactory XOF;

    static {
        final XMLOutputFactory f = XMLOutputFactory.newFactory();
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
        XOF = f;
    }

    private NormalizedNodeXMLOutput() {
    }

    public static void toStream(OutputStream outStream, NormalizedNode<?, ?> node)
            throws XMLStreamException, IOException {
        XMLStreamWriter xmlWriter = XOF.createXMLStreamWriter(outStream);

        IndentingXMLStreamWriter indenting = new IndentingXMLStreamWriter(xmlWriter);
        try (NormalizedNodeStreamWriter streamWriter = XMLStreamNormalizedNodeStreamWriter.createSchemaless(
                indenting)) {
            NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(streamWriter);
            nodeWriter.write(node);
            nodeWriter.flush();
        }
    }

    public static void toFile(File file, NormalizedNode<?, ?> node) {
        try (FileOutputStream outStream = new FileOutputStream(file)) {
            toStream(outStream, node);
        } catch (IOException | XMLStreamException e) {
            LOG.error("Error writing NormalizedNode to file {}", file, e);
        }
    }
}
