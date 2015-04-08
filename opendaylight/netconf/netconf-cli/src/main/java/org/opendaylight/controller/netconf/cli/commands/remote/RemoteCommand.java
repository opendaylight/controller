/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.remote;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.netconf.cli.commands.AbstractCommand;
import org.opendaylight.controller.netconf.cli.commands.Command;
import org.opendaylight.controller.netconf.cli.commands.CommandInvocationException;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.commands.output.OutputDefinition;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Generic remote command implementation that sends the rpc xml to the remote device and waits for response
 * Waiting is limited with TIMEOUT
 */
public class RemoteCommand extends AbstractCommand {

    // TODO make this configurable
    private static final long DEFAULT_TIMEOUT = 10000;
    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final DOMRpcService rpcService;

    public RemoteCommand(final QName qName, final InputDefinition args, final OutputDefinition output, final String description, final DOMRpcService rpcService) {
        super(qName, args, output, description);
        this.rpcService = rpcService;
    }

    @Override
    public Output invoke(final Input inputArgs) throws CommandInvocationException {
        final CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc =
                rpcService.invokeRpc(SchemaPath.create(Collections.singletonList(getCommandId()), true), inputArgs.wrap(getCommandId()));

        try {
            return new Output(invokeRpc.get(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT).getResult());
        } catch (final ExecutionException e) {
            throw new CommandInvocationException(getCommandId(), e);
        } catch (final TimeoutException e) {
            // Request timed out, cancel request
            invokeRpc.cancel(true);
            throw new CommandInvocationException.CommandTimeoutException(getCommandId(), e);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Command fromRpc(final RpcDefinition rpcDefinition, final DOMRpcService rpcService) {
        final InputDefinition args = getInputDefinition(rpcDefinition);
        final OutputDefinition retVal = getOutputDefinition(rpcDefinition);

        return new RemoteCommand(rpcDefinition.getQName(), args, retVal, rpcDefinition.getDescription(), rpcService);
    }
}
