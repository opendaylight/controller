package org.opendaylight.controller.test.sal.util.it;

import static org.junit.Assert.assertEquals;

import org.opendaylight.controller.md.sal.binding.util.AbstractLazyNotificationTransformer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeExperimenterErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SwitchFlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

import com.google.common.collect.ImmutableSet;

class TestLazyNotificationTransformer extends AbstractLazyNotificationTransformer
        implements SalFlowListener {
    public int notificationCount = 0;

    public void assertNotificationCount(int count) {
        assertEquals(count, this.notificationCount);
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        super.onSessionInitiated(session);
    }

    @Override
    public ImmutableSet<Class<? extends Notification>> getProvidedNotifications() {
        // Indicate that this transformer provides PacketReceivedNotifications
        return ImmutableSet.<Class<? extends Notification>>of(PacketReceived.class);
    }

    @Override
    public ImmutableSet<NotificationListener> getConsumedNotificationListeners() {
        // Indicate that this transformer is a notification listener itself
        return ImmutableSet.<NotificationListener>of(this);
    }

    // Listen for FlowAdded, and fire a PacketReceived when we receive it
    @Override
    public void onFlowAdded(FlowAdded notification) {
        this.notificationCount++;
        PacketReceivedBuilder builder = new PacketReceivedBuilder();
        notificationService.publish(builder.build());
    }

    @Override
    public void onFlowRemoved(FlowRemoved notification) {
        // Intentionally ignored as not used in test

    }

    @Override
    public void onFlowUpdated(FlowUpdated notification) {
        // Intentionally ignored as not used in test

    }

    @Override
    public void onNodeErrorNotification(NodeErrorNotification notification) {
        // Intentionally ignored as not used in test

    }

    @Override
    public void onNodeExperimenterErrorNotification(NodeExperimenterErrorNotification notification) {
        // Intentionally ignored as not used in test

    }

    @Override
    public void onSwitchFlowRemoved(SwitchFlowRemoved notification) {
        // Intentionally ignored as not used in test

    }
    //  Allow the test to steal our notification service provider so it can send notifications
    public NotificationProviderService stealNotificationServiceProvider() {
        return getNotificationService();
    }
};
