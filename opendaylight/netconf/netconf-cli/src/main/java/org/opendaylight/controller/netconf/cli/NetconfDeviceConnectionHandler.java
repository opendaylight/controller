/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import com.google.common.base.Optional;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Implementation of RemoteDeviceHandler. Integrates cli with
 * sal-netconf-connector.
 */
public class NetconfDeviceConnectionHandler implements RemoteDeviceHandler<NetconfSessionPreferences> {

    private final CommandDispatcher commandDispatcher;
    private final SchemaContextRegistry schemaContextRegistry;
    private final ConsoleIO console;
    private final String deviceId;

    private boolean up = false;

    public NetconfDeviceConnectionHandler(final CommandDispatcher commandDispatcher,
            final SchemaContextRegistry schemaContextRegistry, final ConsoleIO console, final String deviceId) {
        this.commandDispatcher = commandDispatcher;
        this.schemaContextRegistry = schemaContextRegistry;
        this.console = console;
        this.deviceId = deviceId;
    }

    @Override
    public synchronized void onDeviceConnected(final SchemaContext context,
            final NetconfSessionPreferences preferences, final RpcImplementation rpcImplementation) {
        console.enterRootContext(new ConsoleContext() {

            @Override
            public Optional<String> getPrompt() {
                return Optional.of(deviceId);
            }

            @Override
            public Completer getCompleter() {
                return new NullCompleter();
            }
        });

        // TODO Load schemas for base netconf + inet types from remote device if
        // possible
        // TODO detect netconf base version
        // TODO detect inet types version
        commandDispatcher.addRemoteCommands(rpcImplementation, context);
        schemaContextRegistry.setRemoteSchemaContext(context);
        up = true;
        this.notify();
    }

    /**
     * @return true if connection was fully established
     */
    public synchronized boolean isUp() {
        return up;
    }

    @Override
    public synchronized void onDeviceDisconnected() {
        console.leaveRootContext();
        commandDispatcher.removeRemoteCommands();
        schemaContextRegistry.setRemoteSchemaContext(null);
        up = false;
    }

    @Override
    public void onDeviceFailed(Throwable throwable) {
        // FIXME
    }

    @Override
    public void onNotification(final CompositeNode compositeNode) {
        // FIXME
    }

    @Override
    public void close() {
        // FIXME
    }
}
