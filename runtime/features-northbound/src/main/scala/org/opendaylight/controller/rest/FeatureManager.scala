package org.opendaylight.controller.rest

import akka.actor.Actor
//import org.apache.karaf.features.FeaturesService

case class FeatureInstall(name: String)
case class FeatureUninstall(name: String)
case class FeatureList
case class FeatureInstalled

class FeatureManager extends Actor {
	/**
    var featuresService : FeaturesService = null
    def setFeaturesService(featuresService : FeaturesService) : Unit = {
        this.featuresService = featuresService
    }
	**/
    
    implicit val system = context.system
    
    def receive = {
        case FeatureInstall(x) => 
            println("TODO Install feature " + x)
            //featuresService.installFeature(x)
        case FeatureUninstall(x) => 
            println("TODO Uninstall feature " + x)
            //featuresService.uninstallFeature(x)
        case _ : FeatureList => 
            println("send list of features to RestManager")
        case _ : FeatureInstalled => 
            println("send list of installed features to RestManager")
        case _ => println("Error unknown message")
    }

}