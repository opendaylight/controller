package org.opendaylight.controller.rest

import spray.json.DefaultJsonProtocol

case class Color(name: String, red: Int, green: Int, blue: Int)

case class Feature(id: String, name: String, description: String, details: String,
		version: String, hasVersion: Boolean, resolver: String)
		
case class FeatureName(name: String)

object MyJsonProtocol extends DefaultJsonProtocol {
	implicit val featureFormat = jsonFormat7(Feature)
	implicit val featureNameFormat = jsonFormat1(FeatureName)
}
