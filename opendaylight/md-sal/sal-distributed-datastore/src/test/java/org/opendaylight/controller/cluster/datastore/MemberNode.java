/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Class that represents a cluster member node for unit tests. It encapsulates an actor system with
 * config and (optional) operational data store instances. The Builder is used to specify the setup
 * parameters and create the data store instances. The actor system is automatically joined to address
 * 127.0.0.1:2558 so one member must specify an akka cluster configuration with that address.
 *
 * @author Thomas Pantelis
 */
public class MemberNode {
    static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");

    private IntegrationTestKit kit;
    private DistributedDataStore configDataStore;
    private DistributedDataStore operDataStore;
    private DatastoreContext.Builder datastoreContextBuilder;
    private boolean cleanedUp;

    /**
     * Constructs a Builder.
     *
     * @param members the list to which the resulting MemberNode will be added. This makes it easier for
     *                callers to cleanup instances on test completion.
     * @return a Builder instance
     */
    public static Builder builder(List<MemberNode> members) {
        return new Builder(members);
    }

    public IntegrationTestKit kit() {
        return kit;
    }


    public DistributedDataStore configDataStore() {
        return configDataStore;
    }


    public DistributedDataStore operDataStore() {
        return operDataStore;
    }

    public DatastoreContext.Builder datastoreContextBuilder() {
        return datastoreContextBuilder;
    }

    public void waitForMembersUp(String... otherMembers) {
        Set<String> otherMembersSet = Sets.newHashSet(otherMembers);
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 10) {
            CurrentClusterState state = Cluster.get(kit.getSystem()).state();
            for(Member m: state.getMembers()) {
                if(m.status() == MemberStatus.up() && otherMembersSet.remove(m.getRoles().iterator().next()) &&
                        otherMembersSet.isEmpty()) {
                    return;
                }
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        fail("Member(s) " + otherMembersSet + " are not Up");
    }

    public void waitForMemberDown(String member) {
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 10) {
            CurrentClusterState state = Cluster.get(kit.getSystem()).state();
            for(Member m: state.getUnreachable()) {
                if(member.equals(m.getRoles().iterator().next())) {
                    return;
                }
            }

            for(Member m: state.getMembers()) {
                if(m.status() != MemberStatus.up() && member.equals(m.getRoles().iterator().next())) {
                    return;
                }
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        fail("Member " + member + " is now down");
    }

    public void cleanup() {
        if(!cleanedUp) {
            cleanedUp = true;
            kit.cleanup(configDataStore);
            kit.cleanup(operDataStore);
            kit.shutdownActorSystem(kit.getSystem(), Boolean.TRUE);
        }
    }

    public static void verifyRaftState(DistributedDataStore datastore, String shardName, RaftStateVerifier verifier)
            throws Exception {
        ActorContext actorContext = datastore.getActorContext();

        Future<ActorRef> future = actorContext.findLocalShardAsync(shardName);
        ActorRef shardActor = Await.result(future, Duration.create(10, TimeUnit.SECONDS));

        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 5) {
            OnDemandRaftState raftState = (OnDemandRaftState)actorContext.
                    executeOperation(shardActor, GetOnDemandRaftState.INSTANCE);

            try {
                verifier.verify(raftState);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    public static void verifyRaftPeersPresent(DistributedDataStore datastore, final String shardName,
            String... peerMemberNames) throws Exception {
        final Set<String> peerIds = Sets.newHashSet();
        for(String p: peerMemberNames) {
            peerIds.add(ShardIdentifier.builder().memberName(p).shardName(shardName).
                type(datastore.getActorContext().getDataStoreName()).build().toString());
        }

        verifyRaftState(datastore, shardName, new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertEquals("Peers for shard " + shardName, peerIds, raftState.getPeerAddresses().keySet());
            }
        });
    }

    public static void verifyNoShardPresent(DistributedDataStore datastore, String shardName) {
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 5) {
            Optional<ActorRef> shardReply = datastore.getActorContext().findLocalShard(shardName);
            if(!shardReply.isPresent()) {
                return;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        fail("Shard " + shardName + " is present");
    }

    public static class Builder {
        private final List<MemberNode> members;
        private String moduleShardsConfig;
        private String akkaConfig;
        private String[] waitForshardLeader = new String[0];
        private String testName;
        private SchemaContext schemaContext;
        private boolean createOperDatastore = true;
        private DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder().
                shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(30);

        Builder(List<MemberNode> members) {
            this.members = members;
        }

        /**
         * Specifies the name of the module shards config file. This is required.
         *
         * @return this Builder
         */
        public Builder moduleShardsConfig(String moduleShardsConfig) {
            this.moduleShardsConfig = moduleShardsConfig;
            return this;
        }

        /**
         * Specifies the name of the akka configuration. This is required.
         *
         * @return this Builder
         */
        public Builder akkaConfig(String akkaConfig) {
            this.akkaConfig = akkaConfig;
            return this;
        }

        /**
         * Specifies the name of the test that is appended to the data store names. This is required.
         *
         * @return this Builder
         */
        public Builder testName(String testName) {
            this.testName = testName;
            return this;
        }

        /**
         * Specifies the optional names of the shards to initially wait for a leader to be elected.
         *
         * @return this Builder
         */
        public Builder waitForShardLeader(String... shardNames) {
            this.waitForshardLeader = shardNames;
            return this;
        }

        /**
         * Specifies whether or not to create an operational data store. Defaults to true.
         *
         * @return this Builder
         */
        public Builder createOperDatastore(boolean value) {
            this.createOperDatastore = value;
            return this;
        }

        /**
         * Specifies the SchemaContext for the data stores. Defaults to SchemaContextHelper.full().
         *
         * @return this Builder
         */
        public Builder schemaContext(SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
            return this;
        }

        /**
         * Specifies the DatastoreContext Builder. If not specified, a default instance is used.
         *
         * @return this Builder
         */
        public Builder datastoreContextBuilder(DatastoreContext.Builder builder) {
            datastoreContextBuilder = builder;
            return this;
        }

        public MemberNode build() {
            Preconditions.checkNotNull(moduleShardsConfig, "moduleShardsConfig must be specified");
            Preconditions.checkNotNull(akkaConfig, "akkaConfig must be specified");
            Preconditions.checkNotNull(testName, "testName must be specified");

            if(schemaContext == null) {
                schemaContext = SchemaContextHelper.full();
            }

            MemberNode node = new MemberNode();
            node.datastoreContextBuilder = datastoreContextBuilder;

            ActorSystem system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig(akkaConfig));
            Cluster.get(system).join(MEMBER_1_ADDRESS);

            node.kit = new IntegrationTestKit(system, datastoreContextBuilder);

            String memberName = new ClusterWrapperImpl(system).getCurrentMemberName();
            node.kit.getDatastoreContextBuilder().shardManagerPersistenceId("shard-manager-config-" + memberName);
            node.configDataStore = node.kit.setupDistributedDataStore("config_" + testName, moduleShardsConfig,
                    true, schemaContext, waitForshardLeader);

            if(createOperDatastore) {
                node.kit.getDatastoreContextBuilder().shardManagerPersistenceId("shard-manager-oper-" + memberName);
                node.operDataStore = node.kit.setupDistributedDataStore("oper_" + testName, moduleShardsConfig,
                        true, schemaContext, waitForshardLeader);
            }

            members.add(node);
            return node;
        }
    }

    public static interface RaftStateVerifier {
        void verify(OnDemandRaftState raftState);
    }
}