/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.datastore;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.test.sal.binding.it.TestHelper;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
//@ExamReactorStrategy(PerClass.class)
public class ClusteredDataStoreIT {
    private Logger log = LoggerFactory.getLogger(ClusteredDataStoreIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    @Inject
    @Filter(timeout=60*1000)
    private ClusteredDataStore clusteredDS;

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
                //
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),
                // Set the systemPackages (used by clustering)
                systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
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
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                // needed by statisticsmanager
                mavenBundle("org.opendaylight.controller", "containermanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "containermanager.it.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.services").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.stub").versionAsInProject(),

                // List all the bundles on which the test case depends
                mavenBundle("org.opendaylight.controller", "sal").versionAsInProject(),
                TestHelper.baseModelBundles(),
                TestHelper.configMinumumBundles(),
                TestHelper.bindingIndependentSalBundles(),
                TestHelper.bindingAwareSalBundles(),
                TestHelper.mdSalCoreBundles(),
                mavenBundle("org.opendaylight.controller", "config-api").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal.implementation").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "protocol_plugins.stub").versionAsInProject(),

                mavenBundle("org.osgi", "org.osgi.core").versionAsInProject(),
                // adding new maven bundles
                mavenBundle("org.mockito", "mockito-all").versionAsInProject(),

                // needed by hosttracker
                mavenBundle("org.opendaylight.controller", "clustered-datastore-implementation").versionAsInProject(),
                mavenBundle("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
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
    public void testBundleContextClusteredDS_NotNull() throws Exception {
        ServiceReference serviceReference = bc.getServiceReference(ClusteredDataStore.class);
        ClusteredDataStore store = ClusteredDataStore.class.cast(bc.getService(serviceReference));
        assertNotNull(store);
    }

    @Test
    public void testInjected_ClusteredDS_NotNull() {
        assertNotNull(clusteredDS);
    }

    @Test
    public void requestCommit_readConfigurationData_ShouldVerifyDataAndNoException() {
        DataModification dataModification = mock(DataModification.class);
        HashMap map = new HashMap();
        List list = new ArrayList();
        list.add("key");
        InstanceIdentifier key = new InstanceIdentifier(list);
        map.put(key, mock(CompositeNode.class));
        when(dataModification.getUpdatedConfigurationData()).thenReturn(map);
        DataCommitTransaction dataCommitTrans = clusteredDS.requestCommit(dataModification);
        dataCommitTrans.finish();
        Object value = clusteredDS.readConfigurationData(key);
        Assert.assertNotNull(value);
    }

    @Test(expected = NullPointerException.class)
    public void requestCommit_ShouldThrowException() {
        DataModification dataModification = null;
        DataCommitTransaction dataCommitTrans = clusteredDS.requestCommit(dataModification);
        dataCommitTrans.finish();
    }

    @Test
    public void requestCommit_readOperationalData_ShouldVerifyDataAndNoException() {
        DataModification dataModification = mock(DataModification.class);
        HashMap map = new HashMap();
        List list = new ArrayList();
        list.add("key");
        InstanceIdentifier key = new InstanceIdentifier(list);
        map.put(key, mock(CompositeNode.class));
        when(dataModification.getUpdatedOperationalData()).thenReturn(map);
        DataCommitTransaction dataCommitTrans = clusteredDS.requestCommit(dataModification);
        dataCommitTrans.finish();
        Object value = clusteredDS.readOperationalData(key);
        Assert.assertNotNull(value);
    }

    @Test
    public void requestCommit_readConfigurationData_NonExistingKey_ShouldVerifyNoMappedValueAndNoException() {
        DataModification dataModification = mock(DataModification.class);
        HashMap map = new HashMap();
        List list = new ArrayList();
        list.add("key");
        InstanceIdentifier key = new InstanceIdentifier(list);
        map.put(key, "value");
        when(dataModification.getUpdatedConfigurationData()).thenReturn(map);
        DataCommitTransaction dataCommitTrans = clusteredDS.requestCommit(dataModification);
        dataCommitTrans.finish();
        list = new ArrayList();
        list.add("key1");
        InstanceIdentifier key1 = new InstanceIdentifier(list);

        Object value = clusteredDS.readConfigurationData(key1);
        assertNull(value);
    }

    @Test
    public void requestCommit_readOperationalData_NonExistingKey_ShouldVerifyNoMappedValueAndNoException() {
        DataModification dataModification = mock(DataModification.class);
        HashMap map = new HashMap();
        List list = new ArrayList();
        list.add("key");
        InstanceIdentifier key = new InstanceIdentifier(list);
        map.put(key, mock(CompositeNode.class));
        when(dataModification.getUpdatedOperationalData()).thenReturn(map);
        DataCommitTransaction dataCommitTrans = clusteredDS.requestCommit(dataModification);
        dataCommitTrans.finish();
        list = new ArrayList();
        list.add("key1");
        InstanceIdentifier key1 = new InstanceIdentifier(list);

        Object value = clusteredDS.readOperationalData(key1);
        assertNull(value);
    }

    @Test(expected = NullPointerException.class)
    public void requestCommit_readConfigurationData_WithNullPathShouldThrowException() {
        DataModification dataModification = mock(DataModification.class);
        HashMap map = new HashMap();
        List list = new ArrayList();
        list.add("key");
        InstanceIdentifier key = new InstanceIdentifier(list);
        map.put(key, "value");
        when(dataModification.getUpdatedConfigurationData()).thenReturn(map);
        DataCommitTransaction dataCommitTrans = clusteredDS.requestCommit(dataModification);
        dataCommitTrans.finish();
        Object value = clusteredDS.readConfigurationData(null);
    }

    @Test(expected = NullPointerException.class)
    public void requestCommit_readOperationalData_WithNullPathShouldThrowException() {
        DataModification dataModification = mock(DataModification.class);
        HashMap map = new HashMap();
        List list = new ArrayList();
        list.add("key");
        InstanceIdentifier key = new InstanceIdentifier(list);
        map.put(key, "value");
        when(dataModification.getOriginalOperationalData()).thenReturn(map);
        DataCommitTransaction dataCommitTrans = clusteredDS.requestCommit(dataModification);
        dataCommitTrans.finish();
        Object value = clusteredDS.readOperationalData(null);
    }

}
