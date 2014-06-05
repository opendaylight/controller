package com.eclipsesource.jaxrs.publisher.internal;

import static com.eclipsesource.jaxrs.publisher.ServiceProperties.PUBLISH;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import com.eclipsesource.jaxrs.publisher.ResourceFilter;


public class AllResourceFilter implements ResourceFilter {
  
  static final String ANY_SERVICE_FILTER = "(&(objectClass=*)(!(" + PUBLISH + "=false)))";
  
  private final BundleContext context;

  public AllResourceFilter( BundleContext context ) {
    validateContext( context );
    this.context = context;
  }

  private void validateContext( BundleContext context ) {
    if( context == null ) {
      throw new IllegalArgumentException( "context must not be null" );
    }
  }

  @Override
  public Filter getFilter() {
    try {
      return context.createFilter( ANY_SERVICE_FILTER );
    } catch( InvalidSyntaxException willNotHappen ) {
      throw new IllegalStateException( willNotHappen );
    }
  }
}
