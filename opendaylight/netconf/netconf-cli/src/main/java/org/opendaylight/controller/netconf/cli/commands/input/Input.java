/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.input;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Input arguments for and rpc/command execution
 */
public class Input {

    private final List<NormalizedNode<?, ?>> args;

    private final Map<String, NormalizedNode<?, ?>> nameToArg = new HashMap<>();

    public Input(final List<NormalizedNode<?, ?>> args) {
        // FIXME empty Input should be constructed from static factory method
        if(args.isEmpty()) {
            this.args = Collections.emptyList();
            return;
        }

        final NormalizedNode<?, ?> input = args.iterator().next();
        Preconditions
                .checkArgument(input instanceof DataContainerChild<?, ?>, "Input container has to be of type Data Container Child.");
        this.args = new ArrayList<>((Collection) input.getValue());

        for (final NormalizedNode<?, ?> arg : this.args) {
            nameToArg.put(arg.getNodeType().getLocalName(), arg);
        }
    }

    public NormalizedNode<?, ?> getArg(final String name) {
        return nameToArg.get(name);
    }

    public NormalizedNode<?, ?> wrap(final QName rpcQName) {
        //TODO just add the list as children to the node
        return ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(rpcQName))
                .withValue((Collection) args).build();
    }
}
