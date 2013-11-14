package org.opendaylight.controller.sal.connector.remoterpc.impl;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.Serializable;
import java.net.URI;
import java.util.Set;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;




@RunWith(PaxExam.class)
public class
        RoutingTableInfinispanTestIT {
    private Logger log = LoggerFactory
            .getLogger(RoutingTableInfinispanTestIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    @Inject
    private RoutingTableImpl routingTable = null;

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
                //
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir()
                                + "/src/test/resources/logback.xml"),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),
                // Set the systemPackages (used by clustering)
                systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
                // List framework bundles
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell").versionAsInProject(),
                // List logger bundles
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic")
                        .versionAsInProject(),

                mavenBundle("org.opendaylight.controller", "clustering.services")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.stub")
                        .versionAsInProject(),


                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "sal")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.implementation")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal-infinispan-routingtable")
                        .versionAsInProject(),
                mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3")
                        .versionAsInProject(),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager")
                        .versionAsInProject(), junitBundles());
    }

    private String stateToString(int state) {
        switch (state) {
            case Bundle.ACTIVE:
                return "ACTIVE";
            case Bundle.INSTALLED:
                return "INSTALLED";
            case Bundle.RESOLVED:
                return "RESOLVED";
            case Bundle.UNINSTALLED:
                return "UNINSTALLED";
            default:
                return "Not CONVERTED";
        }
    }

    @Test
  public  void testAddGlobalRoute () throws Exception{
       RoutingIdentifierImpl rii  = new RoutingIdentifierImpl();
       routingTable.addGlobalRoute(rii,"172.27.12.1:5000");

       Set<String> routes = routingTable.getRoutes(rii);

       for(String route:routes){
           Assert.assertEquals(route,"172.27.12.1:5000");
       }

       Thread.sleep(60000);

    }


   class RoutingIdentifierImpl implements RpcRouter.RouteIdentifier,Serializable {

       private final URI namespace = URI.create("http://cisco.com/example");
       private final QName QNAME = new QName(namespace,"global");
       private final QName instance = new QName(URI.create("127.0.0.1"),"local");

       @Override
       public QName getContext() {
           return QNAME;
       }

       @Override
       public QName getType() {
           return QNAME;
       }

       @Override
       public org.opendaylight.yangtools.yang.data.api.InstanceIdentifier getRoute() {
           return InstanceIdentifier.of(instance);
       }
   }





}
