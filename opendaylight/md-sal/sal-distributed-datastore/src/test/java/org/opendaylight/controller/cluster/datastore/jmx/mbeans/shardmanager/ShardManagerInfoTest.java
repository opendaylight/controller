/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shardmanager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.cluster.datastore.ShardManagerTest;
import org.opendaylight.controller.cluster.datastore.ShardManager;
//import java.util.concurrent.TimeoutException;

public class ShardManagerInfoTest {
    private ShardManagerInfo shardManagerInfo;
    private MBeanServer mbeanServer;
    private ObjectName testMBeanName;
    private ShardManager shardManager;

    @Before
    public void setUp() throws Exception {
        List<String> shardList = new ArrayList<>();
        shardManagerInfo = new ShardManagerInfo("member-1", "shard-manager-", "config", shardList);
        shardManagerInfo.registerMBean();
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
        String mbeanName = AbstractMXBean.BASE_JMX_PREFIX + "type=" +
                shardManagerInfo.getMBeanType() + ",Category=" +
                shardManagerInfo.getMBeanCategory() + ",name=" +
                shardManagerInfo.getMBeanName();
        testMBeanName = new ObjectName(mbeanName);
        shardManager = ShardManagerTest.getMockShardManager();
        shardManagerInfo.setShardManager(shardManager);
    }

    @Test(expected=RuntimeException.class)
    public void testAddShardReplicaForInvalidShard() throws Exception {
        try{
        Attribute addReplica = new Attribute("AddShardReplica", "model-inventory");
        mbeanServer.setAttribute(testMBeanName, addReplica);
        } catch (IllegalStateException ex)
        {
        System.out.println ("caught the exception");
        }
    }

    @Test
    public void testAddShardReplica() throws Exception {
        Attribute addReplica = new Attribute("AddShardReplica", "astronauts");
        mbeanServer.setAttribute(testMBeanName, addReplica);
    }

    @Test(expected=RuntimeException.class)
    public void testRemoveShardReplicaForInvalidShard() throws Exception {
        Attribute removeReplica = new Attribute("RemoveShardReplica", "model-inventory");
        mbeanServer.setAttribute(testMBeanName, removeReplica);
    }

    @Test
    public void testRemoveShardReplica() throws Exception {
        Attribute removeReplica = new Attribute("RemoveShardReplica", "astronauts");
        mbeanServer.setAttribute(testMBeanName, removeReplica);
    }

    @Test(expected=RuntimeException.class)
    public void testAddShardReplicaTimeout() throws Exception {
        Attribute addReplica = new Attribute("AddShardReplica", "model-default");
        mbeanServer.setAttribute(testMBeanName, addReplica);
    }


    /*
    @Test
    public void testAddShardReplicaForAlreadyCreatedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

            ActorRef shardManager = getSystem().actorOf(shardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            SchemaContext schemaContext = TestModel.createTestContext();
            shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

            DatastoreContext datastoreContext = DatastoreContext.newBuilder().shardElectionTimeoutFactor(100).
                    persistent(false).build();
            TestShardPropsCreator shardPropsCreator = new TestShardPropsCreator();
            ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("model-inventory"), "modelInventory",
                    "model_inventory", null, Arrays.asList("member-1"));
            shardManager.tell(new CreateShard(config, shardPropsCreator, datastoreContext), getRef());

            Attribute addReplica = new Attribute("addShardReplica", "model_inventory");
            mbeanServer.setAttribute(testMBeanName, addReplica);

            expectMsgClass(duration("2 seconds"), IllegalArgumentException.class);
        }};
    }
    @Test
    public void testAddShardReplica() throws Exception {
        new JavaTestKit(getSystem()) {{
            datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

            ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor);

            Attribute addReplica = new Attribute("addShardReplica", "model_inventory");
            mbeanServer.setAttribute(testMBeanName, addReplica);

            expectNoMsg(FiniteDuration.create(2, TimeUnit.SECONDS));
         }};
    }

    @Test
    public void testRemoveShardReplicaForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = getSystem().actorOf(shardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            Attribute removeReplica = new Attribute("removeShardReplica", "inventory");
            mbeanServer.setAttribute(testMBeanName, removeReplica);

            expectMsgClass(duration("2 seconds"), IllegalArgumentException.class);
        }};

    }
    */
    @After
    public void tearDown() throws Exception {
        shardManagerInfo.unregisterMBean();
    }
}
