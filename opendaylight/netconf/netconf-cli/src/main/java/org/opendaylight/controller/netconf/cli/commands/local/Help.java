/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.local;

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
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

import com.google.common.collect.Lists;

public class Help implements Command {

    private final QName qName;
    private final InputDefinition argsDefinition;
    private final OutputDefinition output;

    // FIXME add command registry as parameter
    public Help(final QName qName, final InputDefinition argsDefinition, final OutputDefinition output) {
        this.argsDefinition = argsDefinition;
        this.qName = qName;
        this.output = output;
    }

    @Override
    public Output invoke(final RpcImplementation rpc, final Input inputArgs) {
        // FIXME implement
        System.err.println("HELP invoked");
        return new Output(new CompositeNodeTOImpl(qName, null, Lists.<Node<?>> newArrayList(new SimpleNodeTOImpl<>(
                qName, null, "Help invoked"))));
    }

    @Override
    public InputDefinition getInputDefinition() {
        return argsDefinition;
    }

    @Override
    public OutputDefinition getOutputDefinition() {
        return output;
    }

    @Override
    public QName getCommandId() {
        return qName;
    }

    public static Command create(final RpcDefinition rpcDefinition) {
        // TODO same code as in RemoteCommand
        final ContainerSchemaNode input = rpcDefinition.getInput();
        final InputDefinition args = input != null ? InputDefinition.fromInput(input) : InputDefinition.empty();

        final ContainerSchemaNode output = rpcDefinition.getOutput();
        final OutputDefinition retVal = output != null ? OutputDefinition.fromOutput(output) : OutputDefinition.empty();
        return new Help(rpcDefinition.getQName(), args, retVal);
    }
}
