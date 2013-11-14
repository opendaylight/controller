package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.osgi.framework.BundleContext;

public class StatisticsManagerActivator extends AbstractBindingAwareProvider {

    private static ProviderContext pSession;
    
    private static StatisticsProvider statsProvider = new StatisticsProvider();
   
    @Override
    public void onSessionInitiated(ProviderContext session) {
        
        DataProviderService dps = session.<DataProviderService>getSALService(DataProviderService.class);
        StatisticsManagerActivator.statsProvider.setDataService(dps);
        NotificationProviderService nps = session.<NotificationProviderService>getSALService(NotificationProviderService.class);
        StatisticsManagerActivator.statsProvider.setNotificationService(nps);
        StatisticsManagerActivator.statsProvider.start();

    }
    
    @Override
    protected void stopImpl(BundleContext context) {
        StatisticsManagerActivator.statsProvider.close();
    }
    
    public static ProviderContext getProviderContext(){
        return pSession;
    }

}
