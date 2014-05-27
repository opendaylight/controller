This is a POC project to validate the Akka Persistence for a Clustered DatastoreThe results have been captured in 

https://wiki.opendaylight.org/view/Clustered_Datastore_using_Akka_POC


How to run?
1. To time persistence of journal - run

mvn clean install exec:java -Dexec.mainClass="org.opendaylight.controller.datastore.clustered.Main" -Dexec.args="p 150000 1" 

This would create a journal in  target/org/opendaylight/controller/datastore/clustered/shard/journal 


2. To time on recovery run  

mvn exec:java -Dexec.mainClass="org.opendaylight.controller.datastore.clustered.Main" -Dexec.args="r 150000 1"

The above will time how much time it took to recover 

3. To time taking snapshot of the 150k families structures (it creates and then takes snapshot) 

mvn clean install exec:java -Dexec.mainClass="org.opendaylight.controller.datastore.clustered.Main" -Dexec.args="ps 150000 1"

The snapshot directory is akka-persistence/target/org/opendaylight/controller/datastore/clustered/shard/snapshots

4. To time recovery purely from snapshot run 

mvn exec:java -Dexec.mainClass="org.opendaylight.controller.datastore.clustered.Main" -Dexec.args="sr 150000 1"

