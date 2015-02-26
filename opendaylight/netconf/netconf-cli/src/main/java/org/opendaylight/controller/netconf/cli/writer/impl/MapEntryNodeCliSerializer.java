/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.ListEntryNodeBaseSerializer;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

final class MapEntryNodeCliSerializer extends ListEntryNodeBaseSerializer<String,MapEntryNode> {

    private final NodeSerializerDispatcher<String> dispatcher;
    private final OutFormatter out;

    MapEntryNodeCliSerializer(final OutFormatter out, final NodeSerializerDispatcher<String> dispatcher) {
        this.out = Preconditions.checkNotNull(out);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
    }

    @Override
    public Iterable<String> serialize(final ListSchemaNode schema, final MapEntryNode node) {
        final StringBuilder output = new StringBuilder();
        out.increaseIndent();
        out.addStringWithIndent(output, node.getNodeType().getLocalName());
        serializeKeysIfPresent(node, output);

        out.openComposite(output);
        out.newLine(output);

        for (final String childOutput : super.serialize(schema, node)) {
            output.append(childOutput);
            out.newLine(output);
        }

        out.closeCompositeWithIndent(output);
        out.newLine(output);
        out.decreaseIndent();
        return Collections.singletonList(output.toString());
    }

    private void serializeKeysIfPresent(final MapEntryNode node, final StringBuilder output) {
        final Map<QName, Object> keyValues = node.getIdentifier().getKeyValues();
        if (keyValues.isEmpty()) {
            return;
        }

        int i = 0;
        output.append(" [");
        for (final Entry<QName, Object> qNameObjectEntry : keyValues.entrySet()) {
            output.append(qNameObjectEntry.getKey().getLocalName());
            output.append("=");
            output.append(qNameObjectEntry.getValue().toString());
            if (++i != keyValues.size()) {
                output.append(", ");
            }
        }
        output.append("]");
    }

    @Override
    protected NodeSerializerDispatcher<String> getNodeDispatcher() {
        return dispatcher;
    }
}