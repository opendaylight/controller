package org.opendaylight.controller.hello

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import scala.io.Source
import java.io.BufferedReader
import java.io.InputStreamReader


class Activator extends BundleActivator{
    def stop(context: BundleContext) {
    }
    def start(context: BundleContext) {
    	//println("Starting System")
    	//val lines = Source.fromFile("hello.txt").mkString
    	//println(lines)
    	val text = this.getClass().getClassLoader()
    			.getResourceAsStream("hello.txt")

    	val text2 = new BufferedReader(new InputStreamReader(text)).readLine()
    	println(text2)
    }
}