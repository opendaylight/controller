/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.DomUtils;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizedNodeWriter extends AbstractWriter<DataSchemaNode> {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodeWriter.class);
    private final OutFormatter out;

    public NormalizedNodeWriter(final ConsoleIO console, final OutFormatter out) {
        super(console);
        this.out = out;
    }

    public void writeInner(final DataSchemaNode dataSchemaNode, final List<NormalizedNode<?, ?>> dataNodes) throws WriteException,
            IOException {
        //Preconditions.checkState(dataNodes.size() == 1);
        // TODO - add getDispatcher method to CnSnToNormalizedNodeParserFactory
        // to be able call dispatchChildElement
        final NormalizedNode<?, ?> dataContainerChild = dataNodes.get(0);

        if (dataContainerChild != null) {
            console.writeLn(serializeToCliOutput(dataContainerChild, dataSchemaNode));
        }

    }

    private String serializeToCliOutput(final NormalizedNode<?, ?> dataContainerChild,
            final DataSchemaNode childSchema) {
        final CliOutputFromNormalizedNodeSerializerFactory factorySerialization = CliOutputFromNormalizedNodeSerializerFactory
                .getInstance(out, DomUtils.defaultValueCodecProvider());
        final NodeSerializerDispatcher<String> dispatcher = factorySerialization.getDispatcher();
        final Iterable<String> result = dispatcher.dispatchChildElement(childSchema, (DataContainerChild<?, ?>) dataContainerChild);

        if (result == null) {
            return "";
        }

        final Iterator<String> output = result.iterator();
        if (!output.hasNext()) {
            return "";
        }

        return output.next();
    }

}
