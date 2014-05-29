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

public class Connect implements Command {

    @Override
    public Output invoke(final RpcImplementation rpc, final Input inputArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputDefinition getInputDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputDefinition getOutputDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QName getCommandId() {
        throw new UnsupportedOperationException();
    }
}
