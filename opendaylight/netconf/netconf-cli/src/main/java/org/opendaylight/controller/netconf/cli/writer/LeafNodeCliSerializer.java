/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer;

import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.LeafNodeBaseSerializer;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

import com.google.common.base.Preconditions;

final class LeafNodeCliSerializer extends LeafNodeBaseSerializer<String> {
    private final XmlCodecProvider codecProvider;
    private final OutFormatter out;

    LeafNodeCliSerializer(final OutFormatter out, final XmlCodecProvider codecProvider) {
        this.out = Preconditions.checkNotNull(out);
        this.codecProvider = Preconditions.checkNotNull(codecProvider);
    }

    @Override
    public String serializeLeaf(final LeafSchemaNode schema, final LeafNode<?> node) {
        final StringBuilder output = new StringBuilder();
        out.increaseIndent();
        output.append("\n");
        output.append(out.indent());
        output.append(node.getNodeType().getLocalName());
        output.append(" ");
        output.append(node.getValue());
        out.decreaseIndent();
        return output.toString();
    }
}