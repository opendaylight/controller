package org.opendaylight.controller.rest

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext

class Activator extends BundleActivator {
    def start(context: BundleContext) {
      println("REST Service started")
    }
    
    def stop(context: BundleContext) {}
}