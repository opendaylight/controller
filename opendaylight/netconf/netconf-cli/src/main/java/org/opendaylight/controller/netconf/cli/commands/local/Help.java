/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.local;

import org.opendaylight.controller.netconf.cli.commands.AbstractCommand;
import org.opendaylight.controller.netconf.cli.commands.Command;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.commands.output.OutputDefinition;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

import com.google.common.collect.Lists;

/**
 * Local Help command
 */
public class Help extends AbstractCommand {

    // FIXME add command registry as parameter
    public Help(final QName qName, final InputDefinition argsDefinition, final OutputDefinition output) {
        super(qName, argsDefinition, output);
    }

    @Override
    public Output invoke(final RpcImplementation rpc, final Input inputArgs) {
        // FIXME implement
        return new Output(new CompositeNodeTOImpl(getCommandId(), null,
                Lists.<Node<?>> newArrayList(new SimpleNodeTOImpl<>(getCommandId(), null, "Help invoked"))));
    }

    public static Command create(final RpcDefinition rpcDefinition) {
        return new Help(rpcDefinition.getQName(), getInputDefinition(rpcDefinition), getOutputDefinition(rpcDefinition));
    }
}
