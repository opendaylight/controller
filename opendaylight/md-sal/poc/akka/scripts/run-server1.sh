mvn clean install -f ../scala-server/pom.xml
java -cp ../scala-server/target/server-1.0-SNAPSHOT-allinone.jar -Dakka.cluster.roles.1=member-1 -Dakka.remote.netty.tcp.port=2555 -Dakka.persistence.journal.leveldb.dir="/tmp/member-1" -Dakka.persistence.snapshot-store.local.dir="/tmp/member-1" org.opendaylight.controller.Server

