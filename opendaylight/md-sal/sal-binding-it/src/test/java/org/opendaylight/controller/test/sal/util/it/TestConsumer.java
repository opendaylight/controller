package org.opendaylight.controller.test.sal.util.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;

class TestConsumer implements BindingAwareConsumer, PacketProcessingListener {
    public NotificationService notificationService;
    public int notificationCount = 0;

    public void assertNotificationCount(int count) {
        assertEquals(count, this.notificationCount);
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        notificationService = session.getSALService(NotificationService.class);
        assertNotNull(notificationService);
        // Note we are intentionally not subscribing here
    }

    @Override
    public void onPacketReceived(PacketReceived notification) {
        this.notificationCount++;
    }
};
