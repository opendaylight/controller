/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.pingdiscovery;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverInput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverOutput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverOutputBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.IcmpDiscoveryService;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileGrp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.Futures;

public class IcmpDiscoveryServiceImpl implements IcmpDiscoveryService {

    private DeviceManager devMgr;
    private IcmpProfileManager profileMgr;

    public void setDevMgr(DeviceManager devMgr, IcmpProfileManager profileMgr) {
        this.devMgr = devMgr;
        this.profileMgr = profileMgr;
    }

    @Override
    public Future<RpcResult<DiscoverOutput>> discover(DiscoverInput input) {
        boolean success = false;

        // Lookup the profile by name, if it exists update it's IpList, else
        // create it.
        ProfileGrp profGrp = profileMgr.startDiscoveryProfile(input);
        if (profGrp != null) {

            // Loop on the list of ip addresses, ping each, and create devices
            // for those responding.
            List<IpAddress> ipList = profGrp.getIpList();

            for (IpAddress ipAddr : ipList) {
                String ipAddrStr = PingService.getIPAddressAsString(ipAddr);

                double pingTimeIsMs = PingService.ping(ipAddrStr, 1,
                        profGrp.getTimeoutSeconds());

                if (pingTimeIsMs >= 0) {
                    success = true;
                    devMgr.createDevice(ipAddrStr);
                }
            }

        }

        // update the profile to completed
        profileMgr.finishDiscoveryProfile(profGrp);

        // TODO - send to a thread pool....
        return Futures.immediateFuture(Rpcs.getRpcResult(success,
                new DiscoverOutputBuilder().setSuccess(success).build(),
                Collections.<RpcError> emptySet()));
    }

}
