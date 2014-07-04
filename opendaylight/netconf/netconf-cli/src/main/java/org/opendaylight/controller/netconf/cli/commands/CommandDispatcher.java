/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.netconf.cli.NetconfDeviceConnectionHandler;
import org.opendaylight.controller.netconf.cli.NetconfDeviceConnectionManager;
import org.opendaylight.controller.netconf.cli.commands.local.Close;
import org.opendaylight.controller.netconf.cli.commands.local.Connect;
import org.opendaylight.controller.netconf.cli.commands.local.Disconnect;
import org.opendaylight.controller.netconf.cli.commands.local.Help;
import org.opendaylight.controller.netconf.cli.commands.remote.RemoteCommand;
import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

/**
 * The registry of available commands local + remote. Created from schema contexts.
 */
public class CommandDispatcher {

    // TODO extract interface

    private final Map<QName, Command> localCommands = Maps.newHashMap();
    private final Map<String, QName> nameToQNameLocal = Maps.newHashMap();

    private final Map<QName, Command> remoteCommands = Maps.newHashMap();
    private final Map<String, QName> nameToQNameRemote = Maps.newHashMap();

    public synchronized Map<QName, Command> getCommands() {
        return Collections.unmodifiableMap(mergeCommands());
    }

    private Map<QName, Command> mergeCommands() {
        // TODO cache this merged map
        return mergeMaps(remoteCommands, localCommands);
    }

    private Map<String, QName> mergeCommandIds() {
        // TODO cache this merged map
        return mergeMaps(nameToQNameRemote, nameToQNameLocal);
    }

    private <K, V> Map<K, V> mergeMaps(final Map<K, V> remoteMap, final Map<K, V> localMap) {
        final Map<K, V> mergedCommands = Maps.newHashMap();
        mergedCommands.putAll(remoteMap);
        mergedCommands.putAll(localMap);
        return mergedCommands;
    }

    public synchronized Set<String> getCommandIds() {
        return mergeCommandIds().keySet();
    }

    public synchronized Set<String> getRemoteCommandIds() {
        return nameToQNameRemote.keySet();
    }

    public synchronized Optional<Command> getCommand(final String nameWithModule) {
        final QName commandQName = mergeCommandIds().get(nameWithModule);
        final Map<QName, Command> qNameCommandMap = mergeCommands();
        if(commandQName == null || qNameCommandMap.containsKey(commandQName) == false) {
            return Optional.absent();
        }

        return Optional.of(qNameCommandMap.get(commandQName));
    }

    public synchronized Optional<Command> getCommand(final QName qName) {
        return Optional.fromNullable(mergeCommands().get(qName));
    }

    private static Optional<Command> getCommand(final Map<String, QName> commandNameMap, final Map<QName, Command> commands, final String nameWithModule) {
        final QName qName = commandNameMap.get(nameWithModule);
        if(qName == null)
            return Optional.absent();

        final Command command = commands.get(qName);
        if(command == null) {
            return Optional.absent();
        }

        return Optional.of(command);
    }

    public static final Collection<String> BASE_NETCONF_SCHEMA_PATHS = Lists.newArrayList("/schema/remote/ietf-netconf.yang",
            "/schema/common/netconf-cli-ext.yang", "/schema/common/ietf-inet-types.yang");

    public synchronized void addRemoteCommands(final RpcImplementation rpcInvoker, final SchemaContext remoteSchema) {
        this.addRemoteCommands(rpcInvoker, remoteSchema, parseSchema(BASE_NETCONF_SCHEMA_PATHS));
    }

    public synchronized void addRemoteCommands(final RpcImplementation rpcInvoker, final SchemaContext remoteSchema, final SchemaContext baseNetconfSchema) {
        for (final SchemaContext context : Lists.newArrayList(remoteSchema, baseNetconfSchema)) {
            for (final Module module : context.getModules()) {
                for (final RpcDefinition rpcDefinition : module.getRpcs()) {
                    final Command command = RemoteCommand.fromRpc(rpcDefinition, rpcInvoker);
                    remoteCommands.put(rpcDefinition.getQName(), command);
                    nameToQNameRemote.put(getCommandName(rpcDefinition, module), rpcDefinition.getQName());
                }
            }
        }
    }

    public synchronized void removeRemoteCommands() {
        remoteCommands.clear();
        nameToQNameRemote.clear();
    }

    public static final Collection<String> LOCAL_SCHEMA_PATHS = Lists.newArrayList("/schema/local/netconf-cli.yang", "/schema/common/netconf-cli-ext.yang",
            "/schema/common/ietf-inet-types.yang");

    public synchronized void addLocalCommands(final NetconfDeviceConnectionManager connectionManager, final SchemaContext localSchema, final Integer connectionTimeout) {
        for (final Module module : localSchema.getModules()) {
            for (final RpcDefinition rpcDefinition : module.getRpcs()) {

                // FIXME make local commands extensible
                // e.g. by yang extension defining java class to be instantiated
                // problem is with command specific resources
                // e.g. Help would need command registry
                final Command localCommand;
                if (rpcDefinition.getQName().equals(CommandConstants.HELP_QNAME)) {
                    localCommand = Help.create(rpcDefinition, this);
                } else if (rpcDefinition.getQName().equals(CommandConstants.CLOSE_QNAME)) {
                    localCommand = Close.create(rpcDefinition);
                } else if (rpcDefinition.getQName().equals(CommandConstants.CONNECT_QNAME)) {
                    localCommand = Connect.create(rpcDefinition, connectionManager, connectionTimeout);
                } else if (rpcDefinition.getQName().equals(CommandConstants.DISCONNECT_QNAME)) {
                    localCommand = Disconnect.create(rpcDefinition, connectionManager);
                } else {
                    throw new IllegalStateException("No command implementation available for local command: " + rpcDefinition.getQName());
                }

                localCommands.put(localCommand.getCommandId(), localCommand);
                nameToQNameLocal.put(getCommandName(rpcDefinition, module), localCommand.getCommandId());
            }
        }
    }

    private static String getCommandName(final RpcDefinition rpcDefinition, final Module module) {
        return IOUtil.qNameToKeyString(rpcDefinition.getQName(), module.getName());
    }

    public static SchemaContext parseSchema(final Collection<String> yangPath) {
        final YangParserImpl yangParserImpl = new YangParserImpl();
        // TODO change deprecated method
        final Set<Module> modules = yangParserImpl.parseYangModelsFromStreams(loadYangs(yangPath));
        return yangParserImpl.resolveSchemaContext(modules);
    }

    private static List<InputStream> loadYangs(final Collection<String> yangPaths) {

        return Lists.newArrayList(Collections2.transform(Lists.newArrayList(yangPaths),
                new Function<String, InputStream>() {
                    @Override
                    public InputStream apply(final String input) {
                        final InputStream resourceAsStream = NetconfDeviceConnectionHandler.class.getResourceAsStream(input);
                        Preconditions.checkNotNull(resourceAsStream, "File %s was null", input);
                        return resourceAsStream;
                    }
                }));
    }
}
