package org.opendaylight.controller.md.inventory.manager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryConsumerImpl extends AbstractBindingAwareProvider implements CommandProvider {
    protected static final Logger logger = LoggerFactory.getLogger(InventoryConsumerImpl.class);
    private static ProviderContext p_session;
    private static DataBrokerService dataBrokerService;
    private static NotificationService notificationService;
    private PortConsumerImpl portImplRef;
    private static DataProviderService dataProviderService;
    private static final String NAMEREGEX = "^[a-zA-Z0-9]+$";

    public static enum operation {
        ADD, DELETE, UPDATE, GET
    };

    private static IClusterContainerServices clusterContainerService = null;
    private static IContainer container;

    @Override
    public void onSessionInitiated(ProviderContext session) {

        InventoryConsumerImpl.p_session = session;

        if (!getDependentModule()) {
            logger.error("Unable to fetch handlers for dependent modules");
            return;
        }

        if (null != session) {
            notificationService = session.getSALService(NotificationService.class);

            if (null != notificationService) {
                dataBrokerService = session.getSALService(DataBrokerService.class);

                if (null != dataBrokerService) {
                    dataProviderService = session.getSALService(DataProviderService.class);

                    if (null != dataProviderService) {
                        portImplRef = new PortConsumerImpl();
                        registerWithOSGIConsole();
                    } else {
                        logger.error("Data Provider Service is down or NULL. "
                                + "Accessing data from configuration data store will not be possible");
                    }

                } else {
                    logger.error("Data Broker Service is down or NULL.");
                }
            } else {
                logger.error("Notification Service is down or NULL.");
            }
        } else {
            logger.error("Consumer session is NULL. Please check if provider is registered");
        }

    }

    public static IClusterContainerServices getClusterContainerService() {
        return clusterContainerService;
    }

    public static void setClusterContainerService(IClusterContainerServices clusterContainerService) {
        InventoryConsumerImpl.clusterContainerService = clusterContainerService;
    }

    public static IContainer getContainer() {
        return container;
    }

    public static void setContainer(IContainer container) {
        InventoryConsumerImpl.container = container;
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this, null);
    }

    private boolean getDependentModule() {
        do {
            clusterContainerService = (IClusterContainerServices) ServiceHelper.getGlobalInstance(
                    IClusterContainerServices.class, this);
            try {
                Thread.sleep(4);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } while (clusterContainerService == null);

        do {

            container = (IContainer) ServiceHelper.getGlobalInstance(IContainer.class, this);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } while (container == null);

        return true;
    }

    public static DataProviderService getDataProviderService() {
        return dataProviderService;
    }

    public PortConsumerImpl getFlowImplRef() {
        return portImplRef;
    }

    public static ProviderContext getProviderSession() {
        return p_session;
    }

    public static NotificationService getNotificationService() {
        return notificationService;
    }

    public static DataBrokerService getDataBrokerService() {
        return dataBrokerService;
    }

    /*
     * OSGI COMMANDS
     */
    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        return help.toString();
    }

    // validations for Port and Queue

    public static boolean isNameValid(String name) {

        // Name validation
        if (name == null || name.trim().isEmpty() || !name.matches(NAMEREGEX)) {
            return false;
        }
        return true;

    }

    public static boolean isL2AddressValid(String mac) {
        if (mac == null) {
            return false;
        }

        Pattern macPattern = Pattern.compile("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");
        Matcher mm = macPattern.matcher(mac);
        if (!mm.matches()) {
            logger.debug("Ethernet address {} is not valid. Example: 00:05:b9:7c:81:5f", mac);
            return false;
        }
        return true;
    }

}
