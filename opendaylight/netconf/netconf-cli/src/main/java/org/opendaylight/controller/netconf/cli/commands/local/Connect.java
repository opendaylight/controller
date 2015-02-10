/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.local;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;
import org.opendaylight.controller.netconf.cli.NetconfDeviceConnectionManager;
import org.opendaylight.controller.netconf.cli.commands.AbstractCommand;
import org.opendaylight.controller.netconf.cli.commands.Command;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.commands.output.OutputDefinition;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Local command to connect to a remote device
 */
public class Connect extends AbstractCommand {

    private final NetconfDeviceConnectionManager connectManager;
    private final Integer connectionTimeout;

    private Connect(final QName qName, final InputDefinition args, final OutputDefinition output,
                    final NetconfDeviceConnectionManager connectManager, final String description, final Integer connectionTimeout) {
        super(qName, args, output, description);
        this.connectManager = connectManager;
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public Output invoke(final Input inputArgs) {
        final NetconfClientConfigurationBuilder config = getConfig(inputArgs);
        return invoke(config, getArgument(inputArgs, "address-name", String.class), inputArgs);
    }

    private Output invoke(final NetconfClientConfigurationBuilder config, final String addressName, final Input inputArgs) {
        final Set<String> remoteCmds = connectManager.connectBlocking(addressName, getAdress(inputArgs), config);

        final ArrayList<Node<?>> output = Lists.newArrayList();
        output.add(new SimpleNodeTOImpl<>(QName.create(getCommandId(), "status"), null, "Connection initiated"));

        for (final String cmdId : remoteCmds) {
            output.add(new SimpleNodeTOImpl<>(QName.create(getCommandId(), "remote-commands"), null, cmdId));
        }

        return new Output(new CompositeNodeTOImpl(getCommandId(), null, output));
    }

    private NetconfClientConfigurationBuilder getConfig(final Input inputArgs) {

        final ReconnectStrategy strategy = getReconnectStrategy();

        final String address = getArgument(inputArgs, "address-name", String.class);
        final Integer port = getArgument(inputArgs, "address-port", Integer.class);
        final String username = getArgument(inputArgs, "user-name", String.class);
        final String passwd = getArgument(inputArgs, "user-password", String.class);

        final InetSocketAddress inetAddress;
        try {
            inetAddress = new InetSocketAddress(InetAddress.getByName(address), port);
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Unable to use address: " + address, e);
        }

        return NetconfClientConfigurationBuilder.create().withAddress(inetAddress)
                .withConnectionTimeoutMillis(connectionTimeout)
                .withReconnectStrategy(strategy)
                .withAuthHandler(new LoginPassword(username, passwd))
                .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH);
    }

    private InetSocketAddress getAdress(final Input inputArgs) {
        final String address = getArgument(inputArgs, "address-name", String.class);
        final InetSocketAddress inetAddress;
        try {
            inetAddress = new InetSocketAddress(InetAddress.getByName(address), getArgument(inputArgs, "address-port", Integer.class));
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Unable to use address: " + address, e);
        }
        return inetAddress;
    }

    private <T> Optional<T> getArgumentOpt(final Input inputArgs, final String argName, final Class<T> type) {
        final QName argQName = QName.create(getCommandId(), argName);
        final Node<?> argumentNode = inputArgs.getArg(argName);
        if (argumentNode == null) {
            return Optional.absent();
        }
        Preconditions.checkArgument(argumentNode instanceof SimpleNode, "Only simple type argument supported, %s",
                argQName);

        final Object value = argumentNode.getValue();
        Preconditions.checkArgument(type.isInstance(value), "Unexpected instance type: %s for argument: %s",
                value.getClass(), argQName);
        return Optional.of(type.cast(value));
    }

    private <T> T getArgument(final Input inputArgs, final String argName, final Class<T> type) {
        final Optional<T> argumentOpt = getArgumentOpt(inputArgs, argName, type);
        Preconditions.checkState(argumentOpt.isPresent(), "Argument: %s is missing but is required", argName);
        return argumentOpt.get();
    }

    public static ReconnectStrategy getReconnectStrategy() {
        // FIXME move to args either start-up args or input nodes for connect or both
        return new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 1000);
    }

    public static Command create(final RpcDefinition rpcDefinition, final NetconfDeviceConnectionManager connectManager, final Integer connectionTimeout) {
        return new Connect(rpcDefinition.getQName(), getInputDefinition(rpcDefinition),
                getOutputDefinition(rpcDefinition), connectManager, rpcDefinition.getDescription(), connectionTimeout);
    }
}
