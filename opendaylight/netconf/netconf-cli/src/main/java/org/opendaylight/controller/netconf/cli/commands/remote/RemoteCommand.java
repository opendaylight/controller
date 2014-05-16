/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.remote;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.netconf.cli.commands.Command;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.commands.output.OutputDefinition;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public class RemoteCommand implements Command {
    private final QName qName;
    private final InputDefinition args;
    private final OutputDefinition output;

    public RemoteCommand(final QName qName, final InputDefinition args, final OutputDefinition output) {
        this.qName = qName;
        this.args = args;
        this.output = output;
    }

    @Override
    public Output invoke(final RpcImplementation rpc, final Input inputArgs) {
        try {
            // FIXME does this block response processing in netconf device ?
            return new Output(rpc.invokeRpc(qName, inputArgs.wrap(qName)).get().getResult());
        } catch (InterruptedException | ExecutionException e) {
            // FIXME
            throw new RuntimeException(e);
        }
    }

    public static Command fromRpc(final RpcDefinition rpcDefinition) {
        final ContainerSchemaNode input = rpcDefinition.getInput();
        final InputDefinition args = input != null ? InputDefinition.fromInput(input) : InputDefinition.empty();

        final ContainerSchemaNode output = rpcDefinition.getOutput();
        final OutputDefinition retVal = output != null ? OutputDefinition.fromOutput(output) : OutputDefinition.empty();
        return new RemoteCommand(rpcDefinition.getQName(), args, retVal);
    }

    @Override
    public InputDefinition getInputDefinition() {
        return args;
    }

    @Override
    public OutputDefinition getOutputDefinition() {
        return output;
    }

    @Override
    public QName getCommandId() {
        return qName;
    }
}
