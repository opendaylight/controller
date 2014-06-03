/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer;

import java.util.Collections;

import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.FromNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

final class MapNodeCliSerializer implements FromNormalizedNodeSerializer<String, MapNode, ListSchemaNode> {

    private final FromNormalizedNodeSerializer<String, MapEntryNode, ListSchemaNode> mapEntrySerializer;

    MapNodeCliSerializer(final MapEntryNodeCliSerializer mapEntrySerializer) {
        this.mapEntrySerializer = mapEntrySerializer;
    }

    @Override
    public Iterable<String> serialize(final ListSchemaNode schema, final MapNode node) {
        final StringBuilder output = new StringBuilder();
        for (final MapEntryNode mapEntryNode : node.getValue()) {
            final Iterable<String> valueFromLeafSetEntry = mapEntrySerializer.serialize(schema, mapEntryNode);
            output.append(valueFromLeafSetEntry.iterator().next());
        }
        return Collections.singletonList(output.toString());
    }
}
