/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.input;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;

/**
 * Input arguments for and rpc/command execution
 */
public class Input {

    private final List<Node<?>> args;

    private final Map<String, Node<?>> nameToArg = new HashMap<String, Node<?>>();

    public Input(final List<Node<?>> args) {
        // FIXME empty Input should be constructed from static factory method
        if(args.isEmpty()) {
            this.args = Collections.emptyList();
            return;
        }

        final Node<?> input = args.iterator().next();
        Preconditions
                .checkArgument(input instanceof CompositeNode, "Input container has to be of type composite node.");
        this.args = ((CompositeNode) input).getValue();

        for (final Node<?> arg : this.args) {
            nameToArg.put(arg.getNodeType().getLocalName(), arg);
        }
    }

    public Node<?> getArg(final String name) {
        return nameToArg.get(name);
    }

    public CompositeNode wrap(final QName rpcQName) {
        return new CompositeNodeTOImpl(rpcQName, null, args);
    }
}
