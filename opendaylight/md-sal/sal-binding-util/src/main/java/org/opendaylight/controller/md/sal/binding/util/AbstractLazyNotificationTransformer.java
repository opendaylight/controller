package org.opendaylight.controller.md.sal.binding.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService.NotificationInterestListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.binding.RpcService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/*
 *  Provide an Abstract Base for writing Providers that consume one set of Notifications and emit another lazily
 *
 *  Lazily in this case means that unless there is a Listener registered for a Notification Provided by this Provider
 *  it does not bother to register to Consume the notifications it needs to do its work.
 *
 *  This Abstract Base is appropriate for use in situations where all work (and all emitted Notifications) are only
 *  triggered by the receipt of some other Notification.
 *
 *  Example:
 *
 *  If one were writing an EthernetDecodeProvider which consumed PacketReceived Notifications, decoded the ethernet parts of the packet
 *  and emitted an EthernetPacketReceived notification, it would make no sense to subscribe to PacketReceived and do the work of decoding if
 *  some other module were not subscribed to your EthernetPacketReceived notifications.  So if you do what is below it will only
 *  subscribe to PacketReceived if someone else subscribes to EthernetPacketReceived.
 *
 *  {@code
 *  public class EthernetDecodeProvider extends AbstractLazyNotificationTransformer implements PacketProcessingListener {
 *     public ImmutableSet<Class<? extends Notification>> getProvidedNotifications() {
 *         return ImmutableSet.<Class<? extends Notification>>of(EthernetPacketReceived.class);
 *     }
 *
 *     public ImmutableSet<NotificationListener> getConsumedNotificationListeners() {
 *        return ImmutableSet.<NotificationListener>of(this);
 *     }
 *
 *     @Override
 *     public void onSessionInitiated(ProviderContext session) {
 *        super.onSessionInitiated(session);
 *        // Your work on startup
 *     }
 *
 *     @Override
 *     public void onPacketReceived(PacketReceived notification) {
 *          // Code to decode ethernet, construct EthernetPacketReceived, and send that notifications
 *     }
 *  }
 *}
 */
public abstract class AbstractLazyNotificationTransformer implements NotificationInterestListener, BindingAwareProvider, AutoCloseable {
    protected NotificationProviderService notificationService;
    protected ImmutableMap<NotificationListener,Registration<NotificationListener>> listenerRegistrations;
    protected ListenerRegistration<NotificationInterestListener> interestListenerRegistration;

    public abstract ImmutableSet<Class<? extends Notification>> getProvidedNotifications();

    public abstract ImmutableSet<NotificationListener> getConsumedNotificationListeners();

    public void onNotificationSubscribtion(Class<? extends Notification> notificationType) {
        if ((getListenerRegistrations() == null || getListenerRegistrations().isEmpty())
                && getProvidedNotifications().contains(notificationType)) {
            Map<NotificationListener,Registration<NotificationListener>> listenerRegistrations = new HashMap<NotificationListener,Registration<NotificationListener>>();
            for (NotificationListener listener : this
                    .getConsumedNotificationListeners()) {
                listenerRegistrations.put(listener,
                        getNotificationService().registerNotificationListener(listener));
            }
            setListenerRegistrations(ImmutableMap.<NotificationListener, Registration<NotificationListener>>copyOf(listenerRegistrations));
        }
    }

    /**
     * Returns a set of provided implementations of YANG modules and their rpcs.
     * For this provider returns the emptySet as we do not provide Rpc  implementations
     * @return Set of provided implementation of YANG modules and their Rpcs
     */
    public Collection<? extends RpcService> getImplementations() {
        return Collections.emptySet();
    }

    /**
     * For this provider returns the emptySet as we do not provide Rpc  implementations
     */
    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        return Collections.emptySet();
    }

    /*
     * Method to initialize the transformer.  Subscribes to receive notifications of
     * new notifications subscriptions.  Does *not* subscribe to listen to the other
     * notifications it consumes until it is notified that there is a subscriber for
     * the notifications it provides.
     * @see org.opendaylight.controller.sal.binding.api.BindingAwareProvider#onSessionInitiated(org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext)
     */
    public void onSessionInitiated(ProviderContext session) {
        setNotificationService(session.<NotificationProviderService> getSALService(NotificationProviderService.class));
        setInterestListenerRegistration(getNotificationService().registerInterestListener(this));
    }


    /*
     * Close the Transformer by closing all of the resources its using
     * @see java.lang.AutoCloseable#close()
     */
    public void close() throws Exception {
        /*
         *  Close the InterestListner Registration thus insuring this object is no longer informed about
         *  new subscribers to notifications it provides
         */
        if (getInterestListenerRegistration() != null) {
            getInterestListenerRegistration().close();
        }
        /*
         *  Close the NotificationListner Registrations thus insuring this object is no longer informed about
         *  new notifications it consumes
         */
        if(getListenerRegistrations() != null) {
            for (Registration<NotificationListener> listenerRegistration : getListenerRegistrations().values()) {
                listenerRegistration.close();
            }
        }
    }

    /*
     * This method is deprecated because a ProviderContext contains a superset of
     * what is available in the ConsumerContext.
     * @see org.opendaylight.controller.sal.binding.api.BindingAwareProvider#onSessionInitialized(org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext)
     */
    public void onSessionInitialized(ConsumerContext session) {
        // NOOP
    }

    protected NotificationProviderService getNotificationService() {
        return notificationService;
    }

    private void setNotificationService(
            NotificationProviderService notificationService) {
        this.notificationService = notificationService;
    }

    protected ImmutableMap<NotificationListener, Registration<NotificationListener>> getListenerRegistrations() {
        return listenerRegistrations;
    }

    private void setListenerRegistrations(ImmutableMap<NotificationListener, Registration<NotificationListener>> listenerRegistrations) {
        this.listenerRegistrations = listenerRegistrations;
    }

    protected ListenerRegistration<NotificationInterestListener> getInterestListenerRegistration() {
        return interestListenerRegistration;
    }

    private void setInterestListenerRegistration(ListenerRegistration<NotificationInterestListener> interestListenerRegistration) {
        this.interestListenerRegistration = interestListenerRegistration;
    }
}
