/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.testkit.TestProbe;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ModuleShardBackendResolverTest extends AbstractShardBackendResolverTest {

    FrontendIdentifier frontendIdentifier = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));
    ClientIdentifier clientIdentifier = ClientIdentifier.create(
            frontendIdentifier, 0);

    ModuleShardBackendResolver resolver = new ModuleShardBackendResolver(clientIdentifier, null);

    @Test
    public void resolveShardForPath() throws Exception {
        Long result = resolver.resolveShardForPath(YangInstanceIdentifier.EMPTY);
        
    }

    @Test
    public void getBackendInfo() throws Exception {

    }

    @Test
    public void refreshBackendInfo() throws Exception {

    }

}