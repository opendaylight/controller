cp ../sanitytest/target/sanitytest-0.4.1-SNAPSHOT.jar ./target/distribution.opendaylight-osgipackage/opendaylight/plugins
pushd ./target/distribution.opendaylight-osgipackage/opendaylight/
./run.sh
success=`echo $?`
popd
rm ./target/distribution.opendaylight-osgipackage/opendaylight/plugins/sanitytest-0.4.1-SNAPSHOT.jar
exit $success

