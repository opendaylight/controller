/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation
 *    ProSyst Software GmbH. - compatibility with OSGi specification 4.2 APIs
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

import com.eclipsesource.jaxrs.publisher.internal.ServiceContainer.ServiceHolder;


public class JAXRSConnector {
  
  private static final String HTTP_SERVICE_PORT_PROPERTY = "org.osgi.service.http.port";
  private static final String RESOURCE_HTTP_PORT_PROPERTY = "http.port";
  private static final String DEFAULT_HTTP_PORT = "80";
  
  private final Object lock = new Object();
  private final ServiceContainer httpServices;
  private final ServiceContainer resources;
  private final Map<HttpService, JerseyContext> contextMap;
  private final BundleContext bundleContext;
  private final List<ServiceHolder> resourceCache;
  private String rootPath;

  JAXRSConnector( BundleContext bundleContext ) {
    this.bundleContext = bundleContext;
    this.httpServices = new ServiceContainer( bundleContext );
    this.resources = new ServiceContainer( bundleContext );
    this.contextMap = new HashMap<HttpService, JerseyContext>();
    this.resourceCache = new ArrayList<ServiceHolder>();
  }
  
  void updatePath( String rootPath ) {
    synchronized( lock ) {
      doUpdatePath( rootPath );
    }
  }

  private void doUpdatePath( String rootPath ) {
    this.rootPath = rootPath;
    ServiceHolder[] services = httpServices.getServices();
    for( ServiceHolder serviceHolder : services ) {
      doRemoveHttpService( ( HttpService )serviceHolder.getService() );
      doAddHttpService( serviceHolder.getReference() );
    }
  }
  
  HttpService addHttpService( ServiceReference reference ) {
    synchronized( lock ) {
      return doAddHttpService( reference );
    }
  }

  HttpService doAddHttpService( ServiceReference reference ) {
    ServiceHolder serviceHolder = httpServices.add( reference );
    HttpService service = ( HttpService )serviceHolder.getService();
    contextMap.put( service, createJerseyContext( service, rootPath ) );
    clearCache();
    return service;
  }

  private void clearCache() {
    ArrayList<ServiceHolder> cache = new ArrayList<ServiceHolder>( resourceCache );
    resourceCache.clear();
    for( ServiceHolder serviceHolder : cache ) {
      registerResource( serviceHolder );
    }
  }

  void removeHttpService( HttpService service ) {
    synchronized( lock ) {
      doRemoveHttpService( service );
    }
  }

  void doRemoveHttpService( HttpService service ) {
    JerseyContext context = contextMap.remove( service );
    if( context != null ) {
      cacheFreedResources( context );
    }
    httpServices.remove( service );
  }

  private void cacheFreedResources( JerseyContext context ) {
    List<Object> freeResources = context.eliminate();
    for( Object resource : freeResources ) {
      resourceCache.add( resources.find( resource ) );
    }
  }

  Object addResource( ServiceReference reference ) {
    synchronized( lock ) {
      return doAddResource( reference );
    }
  }

  private Object doAddResource( ServiceReference reference ) {
    ServiceHolder serviceHolder = resources.add( reference );
    registerResource( serviceHolder );
    return serviceHolder.getService();
  }

  private void registerResource( ServiceHolder serviceHolder ) {
    Object port = getPort( serviceHolder );
    registerResource( serviceHolder, port );
  }

  private Object getPort( ServiceHolder serviceHolder ) {
    Object port = serviceHolder.getReference().getProperty( RESOURCE_HTTP_PORT_PROPERTY );
    if( port == null ) {
      port = bundleContext.getProperty( HTTP_SERVICE_PORT_PROPERTY );
      if( port == null ) {
        port = DEFAULT_HTTP_PORT;
      }
    }
    return port;
  }

  private void registerResource( ServiceHolder serviceHolder, Object port ) {
    HttpService service = findHttpServiceForPort( port );
    if( service != null ) {
      JerseyContext jerseyContext = contextMap.get( service );
      jerseyContext.addResource( serviceHolder.getService() );
    } else {
      cacheResource( serviceHolder );
    }
  }

  private void cacheResource( ServiceHolder serviceHolder ) {
    resourceCache.add( serviceHolder );
  }

  private HttpService findHttpServiceForPort( Object port ) {
    ServiceHolder[] serviceHolders = httpServices.getServices();
    HttpService result = null;
    for( ServiceHolder serviceHolder : serviceHolders ) {
      Object servicePort = getPort( serviceHolder );
      if( servicePort.equals( port ) ) {
        result = ( HttpService )serviceHolder.getService();
      }
    }
    return result;
  }

  void removeResource( Object resource ) {
    synchronized( lock ) {
      doRemoveResource( resource );
    }
  }

  private void doRemoveResource( Object resource ) {
    ServiceHolder serviceHolder = resources.find( resource );
    resourceCache.remove( serviceHolder );
    HttpService httpService = findHttpServiceForPort( getPort( serviceHolder ) );
    removeResourcesFromContext( resource, httpService );
    resources.remove( resource );
  }

  private void removeResourcesFromContext( Object resource, HttpService httpService ) {
    JerseyContext jerseyContext = contextMap.get( httpService );
    if( jerseyContext != null ) {
      jerseyContext.removeResource( resource );
    }
  }

  // For testing purpose
  JerseyContext createJerseyContext( HttpService service, String rootPath ) {
    return new JerseyContext( service, rootPath );
  }
  
}
