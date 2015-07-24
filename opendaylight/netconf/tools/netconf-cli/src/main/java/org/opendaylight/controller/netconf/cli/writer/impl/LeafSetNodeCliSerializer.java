/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import java.util.Collections;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.FromNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

final class LeafSetNodeCliSerializer implements
        FromNormalizedNodeSerializer<String, LeafSetNode<?>, LeafListSchemaNode> {
    private final LeafSetEntryNodeCliSerializer leafSetEntryNodeSerializer;
    private final OutFormatter out;

    LeafSetNodeCliSerializer(final OutFormatter out, final LeafSetEntryNodeCliSerializer leafSetEntryNodeSerializer) {
        this.out = out;
        this.leafSetEntryNodeSerializer = leafSetEntryNodeSerializer;
    }

    @Override
    public Iterable<String> serialize(final LeafListSchemaNode schema, final LeafSetNode<?> node) {
        final StringBuilder output = new StringBuilder();
        out.increaseIndent();
        out.addStringWithIndent(output, node.getNodeType().getLocalName());
        out.openComposite(output);
        out.newLine(output);
        for (final LeafSetEntryNode<?> leafEntryNode : node.getValue()) {
            final Iterable<String> valueFromLeafSetEntry = leafSetEntryNodeSerializer.serialize(schema, leafEntryNode);
            output.append(valueFromLeafSetEntry.iterator().next());
            out.newLine(output);
        }
        out.closeCompositeWithIndent(output);
        out.decreaseIndent();
        return Collections.singletonList(output.toString());
    }
}