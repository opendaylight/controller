/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.osgi;

import com.google.common.base.Optional;
import java.net.InetSocketAddress;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class NetconfSSHActivator implements BundleActivator{

    @Override
    public void start(BundleContext context) throws Exception {
        Optional<InetSocketAddress> sshSocketAddressOptional = NetconfConfigUtil.extractSSHNetconfAddress(context);
        Optional<InetSocketAddress> tcpSocketAddressOptional = NetconfConfigUtil.extractSSHNetconfAddress(context);

        if (sshSocketAddressOptional.isPresent() && tcpSocketAddressOptional.isPresent()){
            NetconfSSHServer.start(sshSocketAddressOptional.get().getPort(),tcpSocketAddressOptional.get());
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }
}
