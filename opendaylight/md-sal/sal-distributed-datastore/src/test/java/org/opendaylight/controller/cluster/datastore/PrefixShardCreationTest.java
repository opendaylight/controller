/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Status.Success;
import akka.testkit.JavaTestKit;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.Shard.Builder;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.messages.CreatePrefixedShard;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.shardstrategy.PrefixShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests prefix shard creation in ShardManager.
 */
public class PrefixShardCreationTest extends AbstractShardManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixShardCreationTest.class);

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    @Test
    public void testPrefixShardCreation() throws Exception {

        LOG.info("testPrefixShardCreation starting");
        new JavaTestKit(getSystem()) {
            {
                datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

                final ActorRef shardManager = actorFactory.createActor(newShardMgrProps(
                        new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

                final SchemaContext schemaContext = TestModel.createTestContext();
                shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

                shardManager.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

                final Builder builder = Shard.builder();

                final CreatePrefixedShard createPrefixedShard = new CreatePrefixedShard(
                        new PrefixShardConfiguration(TEST_ID,
                                PrefixShardStrategy.NAME,
                                Collections.singletonList(MEMBER_1)),
                        datastoreContextBuilder.build(), builder);

                shardManager.tell(createPrefixedShard, getRef());
                expectMsgClass(duration("5 seconds"), Success.class);

                shardManager.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardFound.class);
            }
        };
    }
}