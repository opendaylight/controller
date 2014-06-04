/* ProSyst Software GmbH. - compatibility with OSGi specification 4.2 APIs */
package com.eclipsesource.jaxrs.publisher.internal;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


public class Configuration implements ManagedService {

  static final String CONFIG_SERVICE_PID = "com.eclipsesource.jaxrs.connector";
  static final String ROOT_PROPERTY = "root";
  
  private final JAXRSConnector connector;
  
  public Configuration( JAXRSConnector jaxRsConnector ) {
    this.connector = jaxRsConnector;
  }

  @SuppressWarnings( "rawtypes" )
  @Override
  public void updated( Dictionary properties ) throws ConfigurationException {
    if( properties != null ) {
      Object root = properties.get( ROOT_PROPERTY );
      ensureRootIsPresent( root );
      String rootPath = ( String )root;
      ensureRootIsValid( rootPath );
      connector.updatePath( rootPath );
    }
  }

  private void ensureRootIsValid( String rootPath ) throws ConfigurationException {
    if( !rootPath.startsWith( "/" ) ) {
      throw new ConfigurationException( ROOT_PROPERTY, "Root path does not start with a /" );
    }
  }

  private void ensureRootIsPresent( Object root ) throws ConfigurationException {
    if( root == null || !( root instanceof String ) ) {
      throw new ConfigurationException( ROOT_PROPERTY, "Property is not set or invalid." );
    }
  }
}
