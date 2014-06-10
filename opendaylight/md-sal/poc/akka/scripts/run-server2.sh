mvn clean install -f ../scala-server/pom.xml

java -cp ../scala-server/target/server-1.0-SNAPSHOT-allinone.jar -Dakka.cluster.roles.1=member-2 -Dakka.remote.netty.tcp.port=2556 -Dakka.persistence.journal.leveldb.dir="/tmp/member-2" -Dakka.persistence.snapshot-store.local.dir="/tmp/member-2" org.opendaylight.controller.Server

