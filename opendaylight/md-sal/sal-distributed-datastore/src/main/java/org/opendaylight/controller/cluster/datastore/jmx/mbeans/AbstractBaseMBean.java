package org.opendaylight.controller.cluster.datastore.jmx.mbeans;


import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * All MBeans should extend this class that help in registering and
 * unregistering the MBeans.
 *
 */


public abstract class AbstractBaseMBean {


  public static String BASE_JMX_PREFIX = "org.opendaylight.controller:";
  public static String JMX_TYPE_DISTRIBUTED_DATASTORE = "DistributedDatastore";
  public static String JMX_CATEGORY_SHARD = "Shard";

  private static final Logger LOG = LoggerFactory
      .getLogger(AbstractBaseMBean.class);

  MBeanServer server = ManagementFactory.getPlatformMBeanServer();
  /**
   * gets the MBean ObjectName
   *
   * @return Object name of the MBean
   * @throws MalformedObjectNameException - The bean name does not have the right format.
   * @throws NullPointerException - The bean name is null
   */
  protected ObjectName getMBeanObjectName()
      throws MalformedObjectNameException, NullPointerException {
    ObjectName mbeanName = null;
    String name = BASE_JMX_PREFIX + "type="+getMBeanType()+",Category="+
                                   getMBeanCategory() + ",name="+
                                   getMBeanName();

    mbeanName = new ObjectName(name);
    return mbeanName;
  }

  public boolean registerMBean() {
    boolean registered = false;
    try {
      // Object to identify MBean
      final ObjectName mbeanName = this.getMBeanObjectName();

      Preconditions.checkArgument(mbeanName != null,
          "Object name of the MBean cannot be null");

      LOG.debug("Register MBean {}", mbeanName);

      // unregistered if already registered
      if (server.isRegistered(mbeanName)) {

        LOG.debug("MBean {} found to be already registered", mbeanName);

        try {
          unregisterMBean(mbeanName);
        } catch (Exception e) {

          LOG.warn("unregister mbean {} resulted in exception {} ", mbeanName,
              e);
        }
      }
      server.registerMBean(this, mbeanName);

      LOG.debug("MBean {} registered successfully",
          mbeanName.getCanonicalName());
      registered = true;
    } catch (Exception e) {

      LOG.error("registration failed {}", e);

    }
    return registered;
  }


  public boolean unregisterMBean() {
    boolean unregister = false;
    try {
      ObjectName mbeanName = this.getMBeanObjectName();
      unregister = true;
      unregisterMBean(mbeanName);
    } catch (Exception e) {

      LOG.error("Failed when unregistering MBean {}", e);
    }
    return unregister;
  }

  private void unregisterMBean(ObjectName mbeanName)
      throws MBeanRegistrationException, InstanceNotFoundException {

    server.unregisterMBean(mbeanName);

  }


  /**
   * @return name of bean
   */
  protected abstract String getMBeanName();

  /**
   * @return type of the MBean
   */
  protected abstract String getMBeanType();


  /**
   * @return Category name of teh bean
   */
  protected abstract String getMBeanCategory();

  //require for test cases
  public MBeanServer getMBeanServer() {
    return server;
  }
}
