package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAddedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

public class NoficationTest extends AbstractTest {

    private FlowListener listener1 = new FlowListener();
    private FlowListener listener2 = new FlowListener();

    private Registration<NotificationListener> listener1Reg;
    private Registration<NotificationListener> listener2Reg;

    private NotificationProviderService notifyService;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void notificationTest() throws Exception {
        /**
         * 
         * We register Provider 1 which retrieves Notification Service from MD-SAL
         * 
         */
        AbstractTestProvider provider = new AbstractTestProvider() {
            @Override
            public void onSessionInitiated(ProviderContext session) {
                notifyService = session.getSALService(NotificationProviderService.class);
            }
        };
        broker.registerProvider(provider, getBundleContext());

        /**
         * 
         * We register Consumer 1 which retrieves Notification Service from MD-SAL
         * and registers SalFlowListener as notification listener
         * 
         */
        BindingAwareConsumer consumer1 = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(ConsumerContext session) {
                NotificationService notificationService = session.getSALService(NotificationService.class);
                assertNotNull(notificationService);
                listener1Reg = notificationService.registerNotificationListener(listener1);
            }
        };

        broker.registerConsumer(consumer1, getBundleContext());

        assertNotNull(listener1Reg);

        /**
         * We wait 100ms for to make sure broker threads delivered notifications
         */
        notifyService.publish(flowAdded(0));
        Thread.sleep(100);
        
        /** 
         * We verify one notification was delivered
         * 
         */
        assertEquals(1, listener1.addedFlows.size());
        assertEquals(0, listener1.addedFlows.get(0).getCookie().intValue());

        
        /**
         * We also register second consumerm and it's SalFlowListener
         */
        BindingAwareConsumer consumer2 = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(ConsumerContext session) {
                listener2Reg = session.getSALService(NotificationProviderService.class).registerNotificationListener(
                        listener2);
            }
        };

        broker.registerConsumer(consumer2, getBundleContext());

        /**
         * We publish 3 notifications
         */
        notifyService.publish(flowAdded(5));
        notifyService.publish(flowAdded(10));
        notifyService.publish(flowAdded(2));

        /**
         * We wait 100ms for to make sure broker threads delivered notifications
         */
        Thread.sleep(100);
        
        /** 
         * We verify 3 notification was delivered to both listeners
         * (first one received 4 total, second 3 in total).
         * 
         */

        assertEquals(4, listener1.addedFlows.size());
        assertEquals(3, listener2.addedFlows.size());

        /**
         * We close / unregister second listener
         * 
         */
        listener2Reg.close();
  
        /**
         * 
         * We punblish 5th notification
         */
        notifyService.publish(flowAdded(10));
        
        /**
         * We wait 100ms for to make sure broker threads delivered notifications
         */
        Thread.sleep(100);
        
        /**
         * We verify that first consumer received 5 notifications in total,
         * second consumer only three. Last notification was never received,
         * because it already unregistered listener.
         * 
         */
        assertEquals(5, listener1.addedFlows.size());
        assertEquals(3, listener2.addedFlows.size());

    }

    public static FlowAdded flowAdded(int i) {
        FlowAddedBuilder ret = new FlowAddedBuilder();
        ret.setCookie(BigInteger.valueOf(i));
        return ret.build();
    }

    private static class FlowListener implements SalFlowListener {

        List<FlowAdded> addedFlows = new ArrayList<>();
        List<FlowRemoved> removedFlows = new ArrayList<>();
        List<FlowUpdated> updatedFlows = new ArrayList<>();

        @Override
        public void onFlowAdded(FlowAdded notification) {
            addedFlows.add(notification);
        }

        @Override
        public void onFlowRemoved(FlowRemoved notification) {
            removedFlows.add(notification);
        };

        @Override
        public void onFlowUpdated(FlowUpdated notification) {
            updatedFlows.add(notification);
        }

    }
}
