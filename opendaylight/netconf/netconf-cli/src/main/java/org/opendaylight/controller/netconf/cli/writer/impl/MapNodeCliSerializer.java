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
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.FromNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

final class MapNodeCliSerializer implements FromNormalizedNodeSerializer<String, MapNode, ListSchemaNode> {

    private final FromNormalizedNodeSerializer<String, MapEntryNode, ListSchemaNode> mapEntrySerializer;
    private final OutFormatter out;

    MapNodeCliSerializer(final OutFormatter out, final MapEntryNodeCliSerializer mapEntrySerializer) {
        this.out = Preconditions.checkNotNull(out);
        this.mapEntrySerializer = mapEntrySerializer;
    }

    @Override
    public Iterable<String> serialize(final ListSchemaNode schema, final MapNode node) {
        final StringBuilder output = new StringBuilder();

        out.increaseIndent();
        out.addStringWithIndent(output, node.getNodeType().getLocalName());
        output.append(" ");
        out.openComposite(output);
        out.newLine(output);

        for (final MapEntryNode mapEntryNode : node.getValue()) {
            final Iterable<String> valueFromLeafSetEntry = mapEntrySerializer.serialize(schema, mapEntryNode);
            output.append(valueFromLeafSetEntry.iterator().next());
        }

        out.closeCompositeWithIndent(output);
        out.decreaseIndent();

        return Collections.singletonList(output.toString());
    }
}
