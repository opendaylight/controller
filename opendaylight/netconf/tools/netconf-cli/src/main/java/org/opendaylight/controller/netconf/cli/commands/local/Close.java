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
import org.opendaylight.controller.netconf.cli.commands.CommandInvocationException;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.commands.output.OutputDefinition;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Local command to shut down the cli
 */
public class Close extends AbstractCommand {

    public Close(final QName qName, final InputDefinition args, final OutputDefinition output, final String description) {
        super(qName, args, output, description);
    }

    @Override
    public Output invoke(final Input inputArgs) throws CommandInvocationException {
        // FIXME clean up, close session and then close
        System.exit(0);
        return null;
    }

    public static Command create(final RpcDefinition rpcDefinition) {
        return new Close(rpcDefinition.getQName(), getInputDefinition(rpcDefinition),
                getOutputDefinition(rpcDefinition), rpcDefinition.getDescription());
    }

}
