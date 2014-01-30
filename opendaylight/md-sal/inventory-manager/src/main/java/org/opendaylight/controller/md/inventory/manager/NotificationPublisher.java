package org.opendaylight.controller.md.inventory.manager;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.NodeConnectorRemovedNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.NodeConnectorUpdatedNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.NodeRemovedNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.notification.rev130819.NodeUpdatedNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;

public class NotificationPublisher {

	private static NotificationProviderService notificationProviderService;

	NotificationPublisher() {

	}

	public void onSessionInitiated(ProviderContext session) {
		notificationProviderService = session.getSALService(NotificationProviderService.class);
	}


	void publishNodeConnectorUpdatedNotification(NodeConnectorRef ref, NodeConnectorId id) {

		NodeConnectorUpdatedNotificationBuilder builder = new NodeConnectorUpdatedNotificationBuilder();
		builder.setId(id);
		builder.setNodeConnectorRef(ref);

		notificationProviderService.publish(builder.build());

	}

	void publishNodeUpdatedNotification(NodeRef ref, NodeId id) {
		NodeUpdatedNotificationBuilder builder = new NodeUpdatedNotificationBuilder();
		builder.setNodeRef(ref);
		builder.setId(id);

		notificationProviderService.publish(builder.build());
	}

	void publishNodeRemovedNotification(NodeRef ref) {
		NodeRemovedNotificationBuilder builder = new NodeRemovedNotificationBuilder();
		builder.setNodeRef(ref);

		notificationProviderService.publish(builder.build());
	}

	void publishNodeConnectorRemoveNotification(NodeConnectorRef ref) {
		NodeConnectorRemovedNotificationBuilder builder = new NodeConnectorRemovedNotificationBuilder();
		builder.setNodeConnectorRef(ref);

		notificationProviderService.publish(builder.build());
	}
}
