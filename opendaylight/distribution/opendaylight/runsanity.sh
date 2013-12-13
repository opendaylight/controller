# Inject the sanitytest jar as a controller plugin
cp ./target/dependency/sanitytest*.jar ./target/distribution.opendaylight-osgipackage/opendaylight/plugins

# Store the current working directory in a variable so that we can get back to it later
cwd=`pwd`

# Switch to the distribution folder
cd ./target/distribution.opendaylight-osgipackage/opendaylight/

# Run the controller
./run.sh

# Store the exit value of the controller in a variable
success=`echo $?`

# Switch back to the directory from which this script was invoked
cd $cwd

# Remove the sanitytest jar from the plugins directory
rm ./target/distribution.opendaylight-osgipackage/opendaylight/plugins/sanitytest*.jar

# Exit using the exit code that we had captured earlier after running the controller
exit $success

