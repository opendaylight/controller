package org.opendaylight.controller.md.sal.clustering.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.concurrent.ExecutorService;
import org.junit.Test;
import org.opendaylight.controller.md.sal.clustering.service.listener.LastRoleChangeListener;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.api.rev150407.ShardRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.api.rev150407.ShardRoleChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.service.provider.rev150407.ShardRoleChanged;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;

public class ClusteringServiceTest {

    @Test
    public void testClusteringService() {
        NotificationProviderService notificationProviderService = new MockNotificationProviderService();

        LastRoleChangeListener lastRoleChangeListener = new LastRoleChangeListener();
        notificationProviderService.registerNotificationListener(lastRoleChangeListener);

        ClusteringService service = ClusteringServiceFactory.getFactory().getClusteringServiceInstance(notificationProviderService, lastRoleChangeListener);

        assertNull(lastRoleChangeListener.getLastRoleChanged("member-1-inventory-operational-shard"));

        service.changeShardRole("member-1", "member-1-inventory-operational-shard", ShardRole.NONE, ShardRole.FOLLOWER);
        service.changeShardRole("member-1", "member-1-inventory-operational-shard", ShardRole.FOLLOWER, ShardRole.CANDIDATE);
        service.changeShardRole("member-1", "member-1-inventory-operational-shard", ShardRole.CANDIDATE, ShardRole.LEADER);

        service.changeShardRole("member-1", "member-1-inventory-config-shard", ShardRole.NONE, ShardRole.FOLLOWER);

        ShardRoleChange shardRoleChange = lastRoleChangeListener.getLastRoleChanged("member-1-inventory-operational-shard");
        assertNotNull(shardRoleChange);
        assertEquals("member-1-inventory-operational-shard", shardRoleChange.getShardId());
        assertEquals(ShardRole.LEADER, shardRoleChange.getNewRole());

        assertEquals(2, lastRoleChangeListener.getLastRoleChanges().size());
    }


    class MockNotificationProviderService implements NotificationProviderService {

        LastRoleChangeListener lastRoleChangeListener;

        @Override
        public void publish(Notification notification) {
            lastRoleChangeListener.onShardRoleChanged((ShardRoleChanged) notification);
        }

        @Override
        public void publish(Notification notification, ExecutorService executor) {
            lastRoleChangeListener.onShardRoleChanged((ShardRoleChanged) notification);
        }

        @Override
        public ListenerRegistration<NotificationInterestListener> registerInterestListener(NotificationInterestListener interestListener) {
            return null;
        }

        @Override
        public <T extends Notification> ListenerRegistration<NotificationListener<T>> registerNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {
            return null;
        }

        @Override
        public ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
            lastRoleChangeListener = (LastRoleChangeListener) listener;
            return null;
        }
    }
}
