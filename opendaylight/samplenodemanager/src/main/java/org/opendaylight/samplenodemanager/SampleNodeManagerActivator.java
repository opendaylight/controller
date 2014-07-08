package org.opendaylight.samplenodemanager;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.samplenodeextension.rev140402.SamplenodeextensionService;

public class SampleNodeManagerActivator extends AbstractBindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SampleNodeManagerActivator.class);
    //private OsgiConfigurationService ocs;
    private ProviderContext providerContext;
    private SampleNodeExtensionImpl eisi;
    public SampleNodeManagerActivator() {
        eisi = new SampleNodeExtensionImpl();
        //ocs = new OsgiConfigurationService();
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.providerContext = session;
        providerContext.addRpcImplementation(SamplenodeextensionService.class,eisi);
        DataProviderService dataService = session.<DataProviderService>getSALService(DataProviderService.class);
        eisi.setDataProviderService(dataService);
    }
    @Override
    protected void stopImpl(BundleContext context) {
       LOG.warn("SampleNodeManagerActivator stopImpl");
    }

}
