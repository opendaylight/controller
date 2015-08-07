/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertNotNull;
import akka.actor.ActorRef;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Unit tests for DistributedEntityOwnershipService.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipServiceTest extends AbstractActorTest {
    private static int ID_COUNTER = 1;

    private final String dataStoreType = "config" + ID_COUNTER++;
    private DistributedEntityOwnershipService service;
    private DistributedDataStore dataStore;

    @Before
    public void setUp() throws Exception {
        DatastoreContext datastoreContext = DatastoreContext.newBuilder().dataStoreType(dataStoreType).
                shardInitializationTimeout(10, TimeUnit.SECONDS).build();
        dataStore = new DistributedDataStore(getSystem(), new MockClusterWrapper(),
                new MockConfiguration(Collections.<String, List<String>>emptyMap()), datastoreContext );

        dataStore.onGlobalContextUpdated(TestModel.createTestContext());

        service = new DistributedEntityOwnershipService(dataStore);
    }

    @Test
    public void testEntityOwnershipShardCreated() throws Exception {
        service.start();

        Future<ActorRef> future = dataStore.getActorContext().findLocalShardAsync(
                DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME);
        ActorRef shardActor = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        assertNotNull(DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME + " not found", shardActor);
    }

    @Test
    public void testRegisterCandidate() {
    }

    @Test
    public void testRegisterListener() {
    }
}
