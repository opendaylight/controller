/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.LeafSetEntryNodeBaseSerializer;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

final class LeafSetEntryNodeCliSerializer extends LeafSetEntryNodeBaseSerializer<String> {

    private final OutFormatter out;

    public LeafSetEntryNodeCliSerializer(final OutFormatter out) {
        this.out = out;
    }

    @Override
    protected String serializeLeaf(final LeafListSchemaNode schema, final LeafSetEntryNode<?> node) {
        final StringBuilder output = new StringBuilder();
        out.increaseIndent();
        out.addStringWithIndent(output, node.getValue().toString());
        out.decreaseIndent();
        return output.toString();
    }
}