package org.opendaylight.controller.sal.connector.remoterpc.impl;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
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
        ZeroMQRoutingTableTestIT {
    private Logger log = LoggerFactory
            .getLogger(ZeroMQRoutingTableTestIT.class);

    public static final String ODL = "org.opendaylight.controller";
    public static final String YANG = "org.opendaylight.yangtools";
    public static final String CONTROLLER = "org.opendaylight.controller";
    public static final String YANGTOOLS = "org.opendaylight.yangtools";
    RoutingIdentifierImpl rii  = new RoutingIdentifierImpl();
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    @Inject
    private RoutingTable routingTable = null;

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

                mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.console").versionAsInProject(),
                mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.util").versionAsInProject(),
                mavenBundle("equinoxSDK381",
                        "org.eclipse.osgi.services").versionAsInProject(),
                mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.ds").versionAsInProject(),
                mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.command").versionAsInProject(),
                mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.runtime").versionAsInProject(),
                mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.shell").versionAsInProject(),
                // List logger bundles
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                // List all the bundles on which the test case depends
                mavenBundle(ODL,
                        "clustering.services").versionAsInProject(),

                mavenBundle(ODL, "sal").versionAsInProject(),
                mavenBundle(ODL,
                        "sal.implementation").versionAsInProject(),
                mavenBundle(ODL, "containermanager").versionAsInProject(),
                mavenBundle(ODL,
                        "containermanager.it.implementation").versionAsInProject(),
                mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager.shell").versionAsInProject(),
                mavenBundle("eclipselink", "javax.resource").versionAsInProject(),

                mavenBundle("com.google.guava","guava").versionAsInProject(),
                // List logger bundles
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core")
                        .versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic")
                        .versionAsInProject(),

                mavenBundle(ODL, "clustering.services")
                        .versionAsInProject(),
                mavenBundle(ODL, "clustering.stub")
                        .versionAsInProject(),


                // List all the bundles on which the test case depends
                mavenBundle(ODL, "sal")
                        .versionAsInProject(),
                mavenBundle(ODL, "sal-connector-api")
                        .versionAsInProject(),
                mavenBundle(ODL, "zeromq-routingtable.implementation")
                        .versionAsInProject(),

                mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3")
                        .versionAsInProject(),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager")
                        .versionAsInProject(),

                mavenBundle(ODL,
                        "sal-core-api")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.yangtools","yang-data-api")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.yangtools","yang-model-api")
                        .versionAsInProject(),
                mavenBundle("org.opendaylight.yangtools","yang-binding")
                        .versionAsInProject(),

                mavenBundle(CONTROLLER, "sal-binding-api").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-binding-config").versionAsInProject(),
                mavenBundle(CONTROLLER, "sal-binding-broker-impl").versionAsInProject(), //
                mavenBundle("org.javassist", "javassist").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common-util").versionAsInProject(), //

                mavenBundle(YANGTOOLS, "yang-data-api").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-data-impl").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-model-api").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-model-util").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-parser-api").versionAsInProject(),
                mavenBundle(YANGTOOLS, "yang-parser-impl").versionAsInProject(),


                mavenBundle(YANGTOOLS, "binding-generator-spi").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "binding-model-api").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "binding-generator-util").versionAsInProject(),
                mavenBundle(YANGTOOLS, "yang-parser-impl").versionAsInProject(),
                mavenBundle(YANGTOOLS, "binding-type-provider").versionAsInProject(),
                mavenBundle(YANGTOOLS, "binding-generator-api").versionAsInProject(),
                mavenBundle(YANGTOOLS, "binding-generator-spi").versionAsInProject(),
                mavenBundle(YANGTOOLS, "binding-generator-impl").versionAsInProject(),


                mavenBundle(CONTROLLER, "sal-core-api").versionAsInProject().update(), //
                mavenBundle(CONTROLLER, "sal-broker-impl").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-core-spi").versionAsInProject().update(), //

                mavenBundle(YANGTOOLS + ".thirdparty", "antlr4-runtime-osgi-nohead").versionAsInProject(), //

                mavenBundle(YANG+".thirdparty", "xtend-lib-osgi").versionAsInProject(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(), //
                mavenBundle("org.javassist", "javassist").versionAsInProject(),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), //
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(), //

                mavenBundle(ODL, "sal-common").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-api").versionAsInProject(),//
                mavenBundle(ODL, "sal-common-impl").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-util").versionAsInProject(), //

                mavenBundle(ODL, "config-api").versionAsInProject(), //
                mavenBundle(ODL, "config-manager").versionAsInProject(), //
                mavenBundle("commons-io", "commons-io").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),

                mavenBundle(ODL, "sal-binding-api").versionAsInProject(), //
                mavenBundle(ODL, "sal-binding-config").versionAsInProject(),
                mavenBundle("org.javassist", "javassist").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-util").versionAsInProject(), //

                mavenBundle(YANG, "yang-data-api").versionAsInProject(), //
                mavenBundle(YANG, "yang-data-impl").versionAsInProject(), //
                mavenBundle(YANG, "yang-model-api").versionAsInProject(), //
                mavenBundle(YANG, "yang-model-util").versionAsInProject(), //
                mavenBundle(YANG, "yang-parser-api").versionAsInProject(),
                mavenBundle(YANG, "yang-parser-impl").versionAsInProject(),


                mavenBundle(YANG, "binding-generator-spi").versionAsInProject(), //
                mavenBundle(YANG, "binding-model-api").versionAsInProject(), //
                mavenBundle(YANG, "binding-generator-util").versionAsInProject(),
                mavenBundle(YANG, "yang-parser-impl").versionAsInProject(),
                mavenBundle(YANG, "binding-type-provider").versionAsInProject(),
                mavenBundle(YANG, "binding-generator-api").versionAsInProject(),
                mavenBundle(YANG, "binding-generator-spi").versionAsInProject(),
                mavenBundle(YANG, "binding-generator-impl").versionAsInProject(),


                mavenBundle(ODL, "sal-core-api").versionAsInProject().update(), //
                mavenBundle(ODL, "sal-broker-impl").versionAsInProject(), //
                mavenBundle(ODL, "sal-core-spi").versionAsInProject().update(), //

                mavenBundle(YANG + ".thirdparty", "antlr4-runtime-osgi-nohead").versionAsInProject(), //

                mavenBundle(YANG, "concepts").versionAsInProject(),
                mavenBundle(YANG, "yang-binding").versionAsInProject(), //
                mavenBundle(YANG, "yang-common").versionAsInProject(), //
                mavenBundle(YANG+".thirdparty", "xtend-lib-osgi").versionAsInProject(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(), //
                mavenBundle("org.javassist", "javassist").versionAsInProject(),

                junitBundles());
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

       routingTable.addGlobalRoute(rii,"172.27.12.1:5000");

       Set<String> routes = routingTable.getRoutes(rii);

       for(String route:routes){
           Assert.assertEquals(route,"172.27.12.1:5000");
       }


    }


    @Test
    public  void testDeleteGlobalRoute () throws Exception{

        routingTable.removeGlobalRoute(rii);

        Set<String> routes = routingTable.getRoutes(rii);

        Assert.assertNull(routes);


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

       @Override
       public boolean equals(Object o) {
           if (this == o) return true;
           if (o == null || getClass() != o.getClass()) return false;

           RoutingIdentifierImpl that = (RoutingIdentifierImpl) o;

           if (QNAME != null ? !QNAME.equals(that.QNAME) : that.QNAME != null) return false;
           if (instance != null ? !instance.equals(that.instance) : that.instance != null) return false;
           if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;

           return true;
       }

       @Override
       public int hashCode() {
           int result = namespace != null ? namespace.hashCode() : 0;
           result = 31 * result + (QNAME != null ? QNAME.hashCode() : 0);
           result = 31 * result + (instance != null ? instance.hashCode() : 0);
           return result;
       }
   }





}
