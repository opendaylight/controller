/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands;

import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.OutputDefinition;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public abstract class AbstractCommand implements Command {

    private final QName qName;
    private final InputDefinition args;
    private final OutputDefinition output;

    public AbstractCommand(final QName qName, final InputDefinition args, final OutputDefinition output) {
        this.qName = qName;
        this.args = args;
        this.output = output;
    }

    protected static OutputDefinition getOutputDefinition(final RpcDefinition rpcDefinition) {
        final ContainerSchemaNode output = rpcDefinition.getOutput();
        return output != null ? OutputDefinition.fromOutput(output) : OutputDefinition.empty();
    }

    protected static InputDefinition getInputDefinition(final RpcDefinition rpcDefinition) {
        final ContainerSchemaNode input = rpcDefinition.getInput();
        return InputDefinition.fromInput(input);
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
