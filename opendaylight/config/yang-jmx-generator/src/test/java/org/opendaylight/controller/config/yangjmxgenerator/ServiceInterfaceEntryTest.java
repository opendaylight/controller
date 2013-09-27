/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;

import com.google.common.collect.Sets;

public class ServiceInterfaceEntryTest extends AbstractYangTest {
    public static final String PACKAGE_NAME = "packages.sis";
    public static final List<String> expectedSIEFileNames = toFileNames("[EventBusServiceInterface"
            + ".java, "
            + "ScheduledThreadPoolServiceInterface"
            + ".java, ThreadFactoryServiceInterface.java, ThreadPoolServiceInterface.java]");

    private static final URI THREADS_NAMESPACE;
    private static final Date THREADS_REVISION_DATE;
    static {
        try {
            THREADS_NAMESPACE = new URI(ConfigConstants.CONFIG_NAMESPACE
                    + ":threads");
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
        SimpleDateFormat revisionFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            THREADS_REVISION_DATE = revisionFormat.parse("2013-04-09");
        } catch (ParseException e) {
            throw new Error(e);
        }
    }

    public static final QName EVENTBUS_QNAME = new QName(THREADS_NAMESPACE,
            THREADS_REVISION_DATE, "eventbus");
    public static final QName THREADFACTORY_QNAME = new QName(
            THREADS_NAMESPACE, THREADS_REVISION_DATE, "threadfactory");
    public static final QName THREADPOOL_QNAME = new QName(THREADS_NAMESPACE,
            THREADS_REVISION_DATE, "threadpool");
    public static final QName SCHEDULED_THREADPOOL_QNAME = new QName(
            THREADS_NAMESPACE, THREADS_REVISION_DATE, "scheduled-threadpool");
    public static final QName SCHEDULED_EXECUTOR_SERVICE_QNAME = new QName(
            THREADS_NAMESPACE, THREADS_REVISION_DATE,
            "scheduled-executor-service");
    public static final String SCHEDULED_THREADPOOL_INTERFACE_NAME = "ScheduledThreadPoolServiceInterface";

    public static List<String> toFileNames(String fileNameString) {
        assertThat(fileNameString.startsWith("["), CoreMatchers.is(true));
        assertThat(fileNameString.endsWith("]"), CoreMatchers.is(true));
        fileNameString = fileNameString.substring(1,
                fileNameString.length() - 1);
        return Arrays.asList(fileNameString.split(", "));
    }

    @Test
    public void testCreateFromIdentities() {
        // each identity has to have a base that leads to service-type
        Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                .create(threadsModule, PACKAGE_NAME);
        // expected eventbus, threadfactory, threadpool,
        // scheduled-threadpool,thread-rpc-context
        assertThat(namesToSIEntries.size(), is(expectedSIEFileNames.size()));

        Set<QName> withNoBaseType = Sets.newHashSet(EVENTBUS_QNAME,
                THREADFACTORY_QNAME, THREADPOOL_QNAME,
                SCHEDULED_EXECUTOR_SERVICE_QNAME);
        HashSet<QName> withBaseType = new HashSet<>();
        for (Entry<QName, ServiceInterfaceEntry> entry : namesToSIEntries
                .entrySet()) {
            QName qName = entry.getKey();
            if (withNoBaseType.contains(qName)) {
                ServiceInterfaceEntry sie = namesToSIEntries.get(qName);
                assertNotNull(qName + " not found", sie);
                assertThat(qName + " should have empty base type", sie
                        .getBase().isPresent(), is(false));
                assertThat(sie.getQName(), is(qName));
            } else {
                withBaseType.add(qName);
            }
        }
        // scheduled-threadpool has super type threadpool
        assertThat(withBaseType,
                is(Sets.newHashSet(SCHEDULED_THREADPOOL_QNAME)));
        assertThat(withBaseType.contains(SCHEDULED_THREADPOOL_QNAME), is(true));
        ServiceInterfaceEntry scheduled = namesToSIEntries
                .get(SCHEDULED_THREADPOOL_QNAME);
        assertNotNull(scheduled);
        assertThat(scheduled.getQName(), is(SCHEDULED_THREADPOOL_QNAME));
        ServiceInterfaceEntry threadPool = namesToSIEntries
                .get(THREADPOOL_QNAME);
        assertNotNull(threadPool);
        assertThat("scheduled-threadpool should extend threadpool", scheduled
                .getBase().get(), is(threadPool));

        assertThat(scheduled.getExportedOsgiClassName(),
                is(PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threadpool.ScheduledThreadPool"));
        assertThat(threadPool.getExportedOsgiClassName(),
                is(PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX
                        + ".threadpool.ThreadPool"));

        String expectedDescription = "An extension of the simple pool of threads able to schedule "
                + "work to be executed at some point in time.";
        assertThat(trimInnerSpacesOrNull(scheduled.getNullableDescription()),
                is(expectedDescription));
        assertThat(scheduled.getPackageName(), is(PACKAGE_NAME));
        assertThat(scheduled.getTypeName(),
                is(SCHEDULED_THREADPOOL_INTERFACE_NAME));
        assertThat(scheduled.getFullyQualifiedName(), is(PACKAGE_NAME + "."
                + SCHEDULED_THREADPOOL_INTERFACE_NAME));
    }

    static String trimInnerSpacesOrNull(String input) {
        if (input == null)
            return null;
        return input.replaceAll("\\s{2,}", " ");
    }
}
