/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.input;

import java.util.List;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;

/**
 * Input arguments for and rpc/command execution
 */
public class Input {

    private final List<Node<?>> args;

    public Input(final List<Node<?>> args) {
        this.args = args;
    }

    public CompositeNode wrap(final QName rpcQName) {
        return new CompositeNodeTOImpl(rpcQName, null, args);
    }
}
