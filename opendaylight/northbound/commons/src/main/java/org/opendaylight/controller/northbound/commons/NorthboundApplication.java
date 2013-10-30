/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.DeserializationConfig;
import org.opendaylight.controller.northbound.bundlescanner.IBundleScanService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instance of javax.ws.rs.core.Application used to return the classes
 * that will be instantiated for JAXRS processing. This hooks onto the
 * bundle scanner service to provide JAXB classes to JAX-RS for prorcessing.
 */
@SuppressWarnings("unchecked")
public class NorthboundApplication extends Application {
    public static final String JAXRS_RESOURCES_MANIFEST_NAME = "Jaxrs-Resources";
    public static final String JAXRS_EXCLUDES_MANIFEST_NAME = "Jaxrs-Exclude-Types";
    private static final Logger LOGGER = LoggerFactory.getLogger(NorthboundApplication.class);
    private final Set<Object> _singletons;

    public NorthboundApplication() {
        _singletons = new HashSet<Object>();
        _singletons.add(new ContextResolver<JAXBContext>() {
            JAXBContext jaxbContext = newJAXBContext();
            @Override
            public JAXBContext getContext(Class<?> type) {
                return jaxbContext;
            }

        } );
        _singletons.add(getJsonProvider());
        _singletons.add(new JacksonJsonProcessingExceptionMapper());
    }

    ////////////////////////////////////////////////////////////////
    //  Application overrides
    ////////////////////////////////////////////////////////////////

    @Override
    public Set<Object> getSingletons() {
        return _singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        result.addAll(findJAXRSResourceClasses());
        return result;
    }

    private static final JacksonJaxbJsonProvider getJsonProvider() {
        JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
        jsonProvider.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        return jsonProvider;
    }

    private BundleContext getBundleContext() {
        ClassLoader tlcl = Thread.currentThread().getContextClassLoader();
        Bundle bundle = null;

        if (tlcl instanceof BundleReference) {
            bundle = ((BundleReference) tlcl).getBundle();
        } else {
            LOGGER.warn("Unable to determine the bundle context based on " +
                        "thread context classloader.");
            bundle = FrameworkUtil.getBundle(this.getClass());
        }
        return (bundle == null ? null : bundle.getBundleContext());
    }

    private static final IBundleScanService lookupBundleScanner(BundleContext ctx) {
        ServiceReference svcRef = ctx.getServiceReference(IBundleScanService.class);
        if (svcRef == null) {
            throw new ServiceException("Unable to lookup IBundleScanService");
        }
        return IBundleScanService.class.cast(ctx.getService(svcRef));
    }

    private final JAXBContext newJAXBContext() {
        BundleContext ctx = getBundleContext();
        IBundleScanService svc = lookupBundleScanner(ctx);
        try {
            List<Class<?>> cls = svc.getAnnotatedClasses(ctx,
                    new String[] { XmlRootElement.class.getPackage().getName() },
                    parseManifestEntry(ctx, JAXRS_EXCLUDES_MANIFEST_NAME),
                    true);
            return JAXBContext.newInstance(cls.toArray(new Class[cls.size()]));
        } catch (JAXBException je) {
            LOGGER.error("Error creating JAXBContext", je);
            return null;
        }
    }

    private final Set<Class<?>> findJAXRSResourceClasses() {
        BundleContext ctx = getBundleContext();
        String bundleName = ctx.getBundle().getSymbolicName();
        Set<Class<?>> result = new HashSet<Class<?>>();
        ServiceException recordException = null;
        try {
            IBundleScanService svc = lookupBundleScanner(ctx);
            result.addAll(svc.getAnnotatedClasses(ctx,
                    new String[] { javax.ws.rs.Path.class.getName() },
                    null, false));
        } catch (ServiceException se) {
            recordException = se;
            LOGGER.debug("Error finding JAXRS resource annotated classes in " +
                    "bundle: {} error: {}.", bundleName, se.getMessage());
            // the bundle scan service cannot be lookedup. Lets attempt to
            // lookup the resources from the bundle manifest header
            for (String c : parseManifestEntry(ctx, JAXRS_RESOURCES_MANIFEST_NAME)) {
                try {
                    result.add(ctx.getBundle().loadClass(c));
                } catch (ClassNotFoundException cnfe) {
                    LOGGER.error("Cannot load class: {} in bundle: {} " +
                            "defined as MANIFEST JAX-RS resource", c, bundleName, cnfe);
                }
            }
        }

        if (result.size() == 0) {
            if (recordException != null) {
                throw recordException;
            } else {
                throw new ServiceException("No resource classes found in bundle:" +
                        ctx.getBundle().getSymbolicName());
            }
        }
        return result;
    }

    private final Set<String> parseManifestEntry(BundleContext ctx, String name) {
        Set<String> result = new HashSet<String>();
        Dictionary<String,String> headers = ctx.getBundle().getHeaders();
        String header = headers.get(name);
        if (header != null) {
            for (String s : header.split(",")) {
                s = s.trim();
                if (s.length() > 0) {
                    result.add(s);
                }
            }
        }
        return result;
    }

}
