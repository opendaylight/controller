cp ../sanitytest/target/sanitytest-0.4.1-SNAPSHOT.jar ./target/distribution.opendaylight-osgipackage/opendaylight/plugins
./target/distribution.opendaylight-osgipackage/opendaylight/run.sh
success=`echo $?`
rm ./target/distribution.opendaylight-osgipackage/opendaylight/plugins/sanitytest-0.4.1-SNAPSHOT.jar
exit $success

