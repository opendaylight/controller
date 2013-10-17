package org.opendaylight.controller.sal.binding.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public abstract class AbstractBrokerAwareActivator implements BundleActivator {

    private static final ExecutorService mdActivationPool = Executors.newCachedThreadPool();
    private BundleContext context;
    private ServiceTracker<BindingAwareBroker, BindingAwareBroker> tracker;
    private BindingAwareBroker broker;
    private ServiceTrackerCustomizer<BindingAwareBroker, BindingAwareBroker> customizer = new ServiceTrackerCustomizer<BindingAwareBroker, BindingAwareBroker>() {
        
        @Override
        public BindingAwareBroker addingService(ServiceReference<BindingAwareBroker> reference) {
            broker = context.getService(reference);
            mdActivationPool.execute(new Runnable() {
                
                @Override
                public void run() {
                    onBrokerAvailable(broker, context);;
                }
            });
            return broker;
        }
        
        @Override
        public void modifiedService(ServiceReference<BindingAwareBroker> reference, BindingAwareBroker service) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void removedService(ServiceReference<BindingAwareBroker> reference, BindingAwareBroker service) {
            // TODO Auto-generated method stub
            
        }

    };
    
    
    @Override
    public final void start(BundleContext context) throws Exception {
        this.context = context;
        startImpl(context);
        tracker = new ServiceTracker<>(context, BindingAwareBroker.class, customizer);
        tracker.open();
        
    }


    
    @Override
    public final  void stop(BundleContext context) throws Exception {
        tracker.close();
        stopImpl(context);
    }
    
    
    /**
     * Called when this bundle is started (before
     * {@link #onSessionInitiated(ProviderContext)} so the Framework can perform
     * the bundle-specific activities necessary to start this bundle. This
     * method can be used to register services or to allocate any resources that
     * this bundle needs.
     * 
     * <p>
     * This method must complete and return to its caller in a timely manner.
     * 
     * @param context
     *            The execution context of the bundle being started.
     * @throws Exception
     *             If this method throws an exception, this bundle is marked as
     *             stopped and the Framework will remove this bundle's
     *             listeners, unregister all services registered by this bundle,
     *             and release all services used by this bundle.
     */
    protected void startImpl(BundleContext context) {
        // NOOP
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle. In general, this
     * method should undo the work that the {@code BundleActivator.start} method
     * started. There should be no active threads that were started by this
     * bundle when this bundle returns. A stopped bundle must not call any
     * Framework objects.
     * 
     * <p>
     * This method must complete and return to its caller in a timely manner.
     * 
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is still
     *         marked as stopped, and the Framework will remove the bundle's
     *         listeners, unregister all services registered by the bundle, and
     *         release all services used by the bundle.
     */
    protected void stopImpl(BundleContext context) {
        // NOOP
    }
    

    protected abstract void onBrokerAvailable(BindingAwareBroker broker, BundleContext context);
    
    protected void onBrokerRemoved(BindingAwareBroker broker, BundleContext context) {
        
    }
}
