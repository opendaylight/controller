package org.opendaylight.pingDataListener;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.samplenodeextension.rev140402.SampleTestNode;


public class pingDataListenerActivator extends AbstractBindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(pingDataListenerActivator.class);
    private ProviderContext providerContext;
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    private DataBroker service;


    public pingDataListenerActivator() {
    }


    @Override
    public void onSessionInitiated(ProviderContext session) {

        LOG.info("L2ImplActivator onsessioninitiated");
        this.providerContext = session;
        service = session.getSALService(DataBroker.class);
        start_impl(service);
    }
    //public void start_impl(DataProviderService dataService) {
    public void start_impl(DataBroker dataService) {
        System.out.println("######################################");
        System.out.println("######################################");
        System.out.println("##############start_impl##############");
        pingDataChangeListener wakeupListener = new pingDataChangeListener();
        wakeupListener.setDataProviderService(dataService);
        /*dataChangeListenerRegistration = dbs.registerDataChangeListener(
                InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class).toInstance(),
                wakeupListener);*/
        /*dataChangeListenerRegistration = dbs.registerDataChangeListener(
                InstanceIdentifier.builder(Nodes.class).toInstance(),
                wakeupListener);*/
       dataChangeListenerRegistration = dataService.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION,InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class)
                    .augmentation(SampleTestNode.class).toInstance(),
                wakeupListener,DataChangeScope.SUBTREE);
        System.out.println("##############exiting, registered datachangelistener#################");
    }

    @Override
    protected void stopImpl(BundleContext context) {
       LOG.info("L2ImplActivator stopImpl");
    }
}
