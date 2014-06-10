mvn clean install -f ../akka-supervision-poc/pom.xml
java -cp .:../akka-supervision-poc/target/akka-supervision-poc-1.0-allinone.jar org.opendaylight.controller.datastore.clustered.Main pm 1500 1 

