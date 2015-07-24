package org.opendaylight.controller.config.manager.impl.jmx;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.notifications.ConfigJMXNotification;
import org.opendaylight.controller.config.manager.impl.ConfigRegistryImplMXBean;

/**
 * Thin wrapper over ConfigRegistry emitting JMX notifications
 */
public class JMXNotifierConfigRegistry implements ConfigRegistryImplMXBean {

    private final ConfigRegistryImplMXBean delegate;
    private final NotifierMXBeanImpl notifier;

    public JMXNotifierConfigRegistry(final ConfigRegistryImplMXBean delegate, final MBeanServer mBeanServer) {
        this.delegate = delegate;
        notifier = new NotifierMXBeanImpl();
        registerMBean(notifier, mBeanServer, ConfigJMXNotification.OBJECT_NAME);
    }

    private static void registerMBean(final Object instance, final MBeanServer mbs, final ObjectName on) {
        try {
            mbs.registerMBean(instance, on);
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            throw new IllegalStateException("Unable to register " + instance + " as " + on, e);
        }
    }


    @Override
    public long getVersion() {
        return delegate.getVersion();
    }

    @Override
    public ObjectName beginConfig() {
        return delegate.beginConfig();
    }

    @Override
    public CommitStatus commitConfig(final ObjectName transactionControllerON) throws ConflictingVersionException, ValidationException {
        final CommitStatus commitStatus = delegate.commitConfig(transactionControllerON);
        notifier.notifyCommit();
        return commitStatus;
    }

    @Override
    public List<ObjectName> getOpenConfigs() {
        return delegate.getOpenConfigs();
    }

    @Override
    public boolean isHealthy() {
        return delegate.isHealthy();
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return delegate.getAvailableModuleNames();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return delegate.lookupConfigBeans();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(final String moduleName) {
        return delegate.lookupConfigBeans(moduleName);
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(final String moduleName, final String instanceName) {
        return delegate.lookupConfigBeans(moduleName, instanceName);
    }

    @Override
    public ObjectName lookupConfigBean(final String moduleName, final String instanceName) throws InstanceNotFoundException {
        return delegate.lookupConfigBean(moduleName, instanceName);
    }

    @Override
    public void checkConfigBeanExists(final ObjectName objectName) throws InstanceNotFoundException {
        delegate.checkConfigBeanExists(objectName);
    }

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return delegate.getAvailableModuleFactoryQNames();
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans() {
        return delegate.lookupRuntimeBeans();
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans(final String moduleName, final String instanceName) {
        return delegate.lookupRuntimeBeans(moduleName, instanceName);
    }

    @Override
    public ObjectName lookupConfigBeanByServiceInterfaceName(final String serviceInterfaceQName, final String refName) {
        return delegate.lookupConfigBeanByServiceInterfaceName(serviceInterfaceQName, refName);
    }

    @Override
    public Map<String, Map<String, ObjectName>> getServiceMapping() {
        return delegate.getServiceMapping();
    }

    @Override
    public Map<String, ObjectName> lookupServiceReferencesByServiceInterfaceName(final String serviceInterfaceQName) {
        return delegate.lookupServiceReferencesByServiceInterfaceName(serviceInterfaceQName);
    }

    @Override
    public Set<String> lookupServiceInterfaceNames(final ObjectName objectName) throws InstanceNotFoundException {
        return delegate.lookupServiceInterfaceNames(objectName);
    }

    @Override
    public String getServiceInterfaceName(final String namespace, final String localName) {
        return delegate.getServiceInterfaceName(namespace, localName);
    }

    @Override
    public ObjectName getServiceReference(final String serviceInterfaceQName, final String refName) throws InstanceNotFoundException {
        return delegate.getServiceReference(serviceInterfaceQName, refName);
    }

    @Override
    public void checkServiceReferenceExists(final ObjectName objectName) throws InstanceNotFoundException {
        delegate.checkServiceReferenceExists(objectName);
    }

    public interface NotifierMXBean {}

    public static class NotifierMXBeanImpl extends NotificationBroadcasterSupport implements NotifierMXBean {

        private void notifyCommit() {
            sendNotification(ConfigJMXNotification.afterCommit(this, "commit success"));
        }
    }
}
