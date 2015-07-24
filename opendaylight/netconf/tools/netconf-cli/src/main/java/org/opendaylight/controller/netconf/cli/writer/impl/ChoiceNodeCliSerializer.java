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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.ChoiceNodeBaseSerializer;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;

final class ChoiceNodeCliSerializer extends ChoiceNodeBaseSerializer<String> {
    private final NodeSerializerDispatcher<String> dispatcher;
    private final OutFormatter out;

    ChoiceNodeCliSerializer(final OutFormatter out, final NodeSerializerDispatcher<String> dispatcher) {
        this.out = Preconditions.checkNotNull(out);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
    }

    @Override
    public Iterable<String> serialize(final ChoiceSchemaNode schema, final ChoiceNode node) {
        final StringBuilder output = new StringBuilder();
        out.increaseIndent();
        out.addStringWithIndent(output, "choice ");
        output.append(schema.getQName().getLocalName());
        output.append(" (");
        output.append(detectCase(schema, node));
        output.append(") ");
        out.openComposite(output);
        out.newLine(output);

        for (final String childOutput : super.serialize(schema, node)) {
            output.append(childOutput);
            out.newLine(output);
        }

        out.closeCompositeWithIndent(output);
        out.decreaseIndent();
        return Collections.singletonList(output.toString());
    }

    private String detectCase(final ChoiceSchemaNode schema, final ChoiceNode node) {
        for (final DataContainerChild<? extends PathArgument, ?> caseChild : node.getValue()) {
            final QName presentChildQName = caseChild.getNodeType();
            for (final ChoiceCaseNode choiceCaseNode : schema.getCases()) {
                if (choiceCaseNode.getDataChildByName(presentChildQName) != null) {
                    // Pick the first case that contains first child node
                    return choiceCaseNode.getQName().getLocalName();
                }
            }
        }

        // Should not happen, nodes should come from one of the cases
        throw new IllegalStateException("Choice node " + node + " does not conform to choice schema " + schema);
    }

    @Override
    protected NodeSerializerDispatcher<String> getNodeDispatcher() {
        return dispatcher;
    }
}
