/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

public class DeviceCliHandler implements RemoteDeviceHandler<NetconfSessionCapabilities> {

    @Override
    public void onDeviceConnected(final SchemaContextProvider contextProvider,
            final NetconfSessionCapabilities capabilities, final RpcImplementation rpcImplementation) {
        // TODO Load schemas for base netconf + inet types from remote device if
        // possible
        // TODO detect netconf base version
        // TODO detect inet types version
        final SchemaContext schema = parseSchema("/schema/remote/ietf-netconf.yang", "/schema/remote/ietf-netconf-cli.yang",
                "/schema/remote/ietf-inet-types.yang");

        final SchemaContext schemaL = parseSchema("/schema/local/netconf-cli.yang");
        final CommandDispatcher commandRegistry = new NetconfCliInitializer().initCommands(schemaL, schema, contextProvider.getSchemaContext());
        new Cli(commandRegistry, rpcImplementation).run();
    }

    @Override
    public void onDeviceDisconnected() {
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

    private static SchemaContext parseSchema(final String... yangPath) {
        final YangParserImpl yangParserImpl = new YangParserImpl();
        final Set<Module> modules = yangParserImpl.parseYangModelsFromStreams(loadYangs(yangPath));
        return yangParserImpl.resolveSchemaContext(modules);
    }

    private static List<InputStream> loadYangs(final String... yangPaths) {

        return Lists.newArrayList(Collections2.transform(Lists.newArrayList(yangPaths),
                new Function<String, InputStream>() {
                    @Override
                    public InputStream apply(final String input) {
                        final InputStream resourceAsStream = DeviceCliHandler.class.getResourceAsStream(input);
                        Preconditions.checkNotNull(resourceAsStream, "File %s was null", input);
                        return resourceAsStream;
                    }
                }));
    }

}
