/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer;

import java.util.Collections;

import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.ContainerNodeBaseSerializer;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;

import com.google.common.base.Preconditions;

final class ContainerNodeCliSerializer extends ContainerNodeBaseSerializer<String> {

    private final NodeSerializerDispatcher<String> dispatcher;
    private final OutFormatter out;

    ContainerNodeCliSerializer(final OutFormatter out, final NodeSerializerDispatcher<String> dispatcher) {
        this.out = Preconditions.checkNotNull(out);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
    }

    @Override
    public Iterable<String> serialize(final ContainerSchemaNode schema, final ContainerNode containerNode) {
        final StringBuilder output = new StringBuilder();
        out.increaseIndent();
        output.append("\n");
        output.append(out.indent());
        output.append(containerNode.getNodeType().getLocalName());
        output.append(" {");

        for (final String childOutput : super.serialize(schema, containerNode)) {
            output.append(childOutput);
        }
        output.append("\n");
        output.append(out.indent());
        output.append("}");
        out.decreaseIndent();
        return Collections.singletonList(output.toString());
    }

    @Override
    protected NodeSerializerDispatcher<String> getNodeDispatcher() {
        return dispatcher;
    }

}