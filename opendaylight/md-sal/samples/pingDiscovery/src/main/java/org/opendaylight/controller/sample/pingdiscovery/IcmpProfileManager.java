/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.pingdiscovery;

import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverInput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileGrp;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileId;

public interface IcmpProfileManager {
    ProfileGrp startDiscoveryProfile( final DiscoverInput profileInput );

    void finishDiscoveryProfile(ProfileId profId);
}
