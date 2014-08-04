package org.opendaylight.controller.featuresservice;

import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.featuresservice.northbound.FeaturesServiceNorthbound;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private ServiceRegistration registration;

    @Override
    public void start(final BundleContext context) throws Exception {
        //     ServiceReference[] references =
        //     context.getServiceReferences(FeaturesService.class.getName(), null);
        //     FeaturesService service = (FeaturesService)context.getService(references[0]);
        final FeaturesServiceNorthbound featureService = new FeaturesServiceNorthbound();

        ServiceListener servListener = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                ServiceReference sevRef = event.getServiceReference();
                switch(event.getType()) {
                case ServiceEvent.REGISTERED:
                {
                    FeaturesService fService = (FeaturesService)context.getService(sevRef);
                    featureService.setFeaturesService(fService);
                }
                break;
                default:
                    break;
                }
            }
        };

        String filter = "(objectclass=" + FeaturesService.class.getName() + ")";
        try {
            context.addServiceListener(servListener, filter);
            ServiceReference[] servRefList = context.getServiceReferences(FeaturesService.class.getName(), filter);
            for(int i = 0; servRefList != null && i < servRefList.length; i++) {
                servListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED,
                        servRefList[i]));
            }

        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }


        while(featureService.getFeaturesService() == null) {
            System.out.println("feature service is null");
            Thread.sleep(100);
        }
        registration = context.registerService(
                FeaturesServiceNorthbound.class.getName(), featureService, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        registration.unregister();
    }
}
