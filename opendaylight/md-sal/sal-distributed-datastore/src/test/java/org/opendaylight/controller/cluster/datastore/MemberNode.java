/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.AddressFromURIString;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.ClusterEvent.CurrentClusterState;
import org.apache.pekko.cluster.Member;
import org.apache.pekko.cluster.MemberStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Class that represents a cluster member node for unit tests. It encapsulates an actor system with
 * config and (optional) operational data store instances. The Builder is used to specify the setup
 * parameters and create the data store instances. The actor system is automatically joined to address
 * 127.0.0.1:2558 so one member must specify an akka cluster configuration with that address.
 *
 * @author Thomas Pantelis
 */
public class MemberNode {
    private static final String MEMBER_1_ADDRESS = "pekko://cluster-test@127.0.0.1:2558";

    private IntegrationTestKit kit;
    private ClientBackedDataStore configDataStore;
    private ClientBackedDataStore operDataStore;
    private DatastoreContext.Builder datastoreContextBuilder;
    private boolean cleanedUp;

    /**
     * Constructs a Builder.
     *
     * @param members the list to which the resulting MemberNode will be added. This makes it easier for
     *                callers to cleanup instances on test completion.
     * @return a Builder instance
     */
    public static Builder builder(final Path stateDir, final List<MemberNode> members) {
        return new Builder(stateDir, members);
    }

    public IntegrationTestKit kit() {
        return kit;
    }


    public ClientBackedDataStore configDataStore() {
        return configDataStore;
    }


    public ClientBackedDataStore operDataStore() {
        return operDataStore;
    }

    public DatastoreContext.Builder datastoreContextBuilder() {
        return datastoreContextBuilder;
    }

    public void waitForMembersUp(final String... otherMembers) {
        kit.waitForMembersUp(otherMembers);
    }

    public void waitForMemberDown(final String member) {
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            CurrentClusterState state = Cluster.get(kit.getSystem()).state();

            for (Member m : state.getUnreachable()) {
                if (member.equals(m.getRoles().iterator().next())) {
                    return;
                }
            }

            for (Member m : state.getMembers()) {
                if (m.status() != MemberStatus.up() && member.equals(m.getRoles().iterator().next())) {
                    return;
                }
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        fail("Member " + member + " is now down");
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void cleanup() {
        if (!cleanedUp) {
            cleanedUp = true;
            if (configDataStore != null) {
                configDataStore.close();
            }
            if (operDataStore != null) {
                operDataStore.close();
            }

            try {
                IntegrationTestKit.shutdownActorSystem(kit.getSystem(), true);
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(MemberNode.class).warn("Failed to shutdown actor system", e);
            }
        }
    }

    public static void verifyRaftState(final ClientBackedDataStore datastore, final String shardName,
            final RaftStateVerifier verifier) throws Exception {
        ActorUtils actorUtils = datastore.getActorUtils();

        Future<ActorRef> future = actorUtils.findLocalShardAsync(shardName);
        ActorRef shardActor = Await.result(future, FiniteDuration.create(10, TimeUnit.SECONDS));

        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            OnDemandRaftState raftState = (OnDemandRaftState)actorUtils
                    .executeOperation(shardActor, GetOnDemandRaftState.INSTANCE);

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

    public static void verifyRaftPeersPresent(final ClientBackedDataStore datastore, final String shardName,
            final String... peerMemberNames) throws Exception {
        final Set<String> peerIds = new HashSet<>();
        for (String p: peerMemberNames) {
            peerIds.add(ShardIdentifier.create(shardName, MemberName.forName(p),
                datastore.getActorUtils().getDataStoreName()).toString());
        }

        verifyRaftState(datastore, shardName, raftState -> assertEquals("Peers for shard " + shardName, peerIds,
            raftState.getPeerAddresses().keySet()));
    }

    public static void verifyNoShardPresent(final ClientBackedDataStore datastore, final String shardName) {
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            Optional<ActorRef> shardReply = datastore.getActorUtils().findLocalShard(shardName);
            if (!shardReply.isPresent()) {
                return;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        fail("Shard " + shardName + " is present");
    }

    public static class Builder {
        private final @NonNull Path stateDir;
        private final List<MemberNode> members;
        private String moduleShardsConfig;
        private String akkaConfig;
        private boolean useAkkaArtery = true;
        private String[] waitForshardLeader = new String[0];
        private String testName;
        private EffectiveModelContext schemaContext;
        private boolean createOperDatastore = true;
        private DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder()
                .shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(30);

        Builder(final Path stateDir, final List<MemberNode> members) {
            this.stateDir = requireNonNull(stateDir);
            this.members = members;
        }

        /**
         * Specifies the name of the module shards config file. This is required.
         *
         * @return this Builder
         */
        public Builder moduleShardsConfig(final String newModuleShardsConfig) {
            moduleShardsConfig = newModuleShardsConfig;
            return this;
        }

        /**
         * Specifies the name of the akka configuration. This is required.
         *
         * @return this Builder
         */
        public Builder akkaConfig(final String newAkkaConfig) {
            akkaConfig = newAkkaConfig;
            return this;
        }

        /**
         * Specifies whether or not to use akka artery for remoting. Default is true.
         *
         * @return this Builder
         */
        public Builder useAkkaArtery(final boolean newUseAkkaArtery) {
            useAkkaArtery = newUseAkkaArtery;
            return this;
        }

        /**
         * Specifies the name of the test that is appended to the data store names. This is required.
         *
         * @return this Builder
         */
        public Builder testName(final String newTestName) {
            testName = newTestName;
            return this;
        }

        /**
         * Specifies the optional names of the shards to initially wait for a leader to be elected.
         *
         * @return this Builder
         */
        public Builder waitForShardLeader(final String... shardNames) {
            waitForshardLeader = shardNames;
            return this;
        }

        /**
         * Specifies whether or not to create an operational data store. Defaults to true.
         *
         * @return this Builder
         */
        public Builder createOperDatastore(final boolean value) {
            createOperDatastore = value;
            return this;
        }

        /**
         * Specifies the SchemaContext for the data stores. Defaults to SchemaContextHelper.full().
         *
         * @return this Builder
         */
        public Builder schemaContext(final EffectiveModelContext newSchemaContext) {
            schemaContext = newSchemaContext;
            return this;
        }

        /**
         * Specifies the DatastoreContext Builder. If not specified, a default instance is used.
         *
         * @return this Builder
         */
        public Builder datastoreContextBuilder(final DatastoreContext.Builder builder) {
            datastoreContextBuilder = builder;
            return this;
        }

        public MemberNode build() throws Exception {
            requireNonNull(moduleShardsConfig, "moduleShardsConfig must be specified");
            requireNonNull(akkaConfig, "akkaConfig must be specified");
            requireNonNull(testName, "testName must be specified");

            if (schemaContext == null) {
                schemaContext = SchemaContextHelper.full();
            }

            MemberNode node = new MemberNode();
            node.datastoreContextBuilder = datastoreContextBuilder;

            Config baseConfig = ConfigFactory.load();
            Config config;
            if (useAkkaArtery) {
                config = baseConfig.getConfig(akkaConfig);
            } else {
                config = baseConfig.getConfig(akkaConfig + "-without-artery")
                        .withFallback(baseConfig.getConfig(akkaConfig));
            }

            ActorSystem system = ActorSystem.create("cluster-test", config);
            String member1Address = useAkkaArtery ? MEMBER_1_ADDRESS : MEMBER_1_ADDRESS.replace("pekko", "pekko.tcp");
            Cluster.get(system).join(AddressFromURIString.parse(member1Address));

            node.kit = new IntegrationTestKit(stateDir, system, datastoreContextBuilder);

            String memberName = new ClusterWrapperImpl(system).getCurrentMemberName().getName();
            node.kit.getDatastoreContextBuilder().shardManagerPersistenceId("shard-manager-config-" + memberName);
            node.configDataStore = node.kit.setupDataStore(ClientBackedDataStore.class, "config_" + testName,
                moduleShardsConfig, true, schemaContext, waitForshardLeader);

            if (createOperDatastore) {
                node.kit.getDatastoreContextBuilder().shardManagerPersistenceId("shard-manager-oper-" + memberName);
                node.operDataStore = node.kit.setupDataStore(ClientBackedDataStore.class,
                        "oper_" + testName, moduleShardsConfig, true, schemaContext, waitForshardLeader);
            }

            members.add(node);
            return node;
        }
    }

    public interface RaftStateVerifier {
        void verify(OnDemandRaftState raftState);
    }
}
