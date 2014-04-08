Use
GROUP_ID=com.mycompany.app
ARTIFACT_ID=my-app
mvn archetype:generate -DgroupId=$GROUP_ID -DartifactId=$ARTIFACT_ID \
 -DarchetypeArtifactId=config-module-archetype -DarchetypeGroupId=org.opendaylight.controller  \
 -DarchetypeVersion=0.2.5-SNAPSHOT

Module name and prefix define yang module name and its java name prefix.
For example when creating thread factory wrapper, yang name might be
thread-factory
and java prefix
ThreadFactory