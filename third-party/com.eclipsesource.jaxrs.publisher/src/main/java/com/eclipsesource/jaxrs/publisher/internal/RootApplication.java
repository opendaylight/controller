/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation
 *    Dragos Dascalita  - added properties
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;


public class RootApplication extends Application {
  
  private final Map<String, Object> properties;
  private final List<Object> resources;

  public RootApplication() {
    resources = new LinkedList<Object>();
    properties = new HashMap<String, Object>();
  }
  
  void addResource( Object resource ) {
    resources.add( resource );
  }
  
  void removeResource( Object resource ) {
    resources.remove( resource );
  }
  
  boolean hasResources() {
    return !resources.isEmpty();
  }

  @Override
  public Set<Object> getSingletons() {
    Set<Object> singletons = new HashSet<Object>( super.getSingletons() );
    singletons.addAll( resources );
    return singletons;
  }

  @Override
  public Map<String, Object> getProperties() {
    return properties;
  }

  public void addProperty( String key, Object value ) {
    properties.put( key, value );
  }
  
}
