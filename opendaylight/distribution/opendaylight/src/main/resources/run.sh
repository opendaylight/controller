#!/bin/bash

[[ -z ${JAVA_HOME} ]] && echo "Need to set JAVA_HOME environment variable" && exit -1;
[[ ! -x ${JAVA_HOME}/bin/java ]] && echo "Cannot find an executable \
JVM at path ${JAVA_HOME}/bin/java check your JAVA_HOME" && exit -1;

platform='unknown'
unamestr=`uname`
if [[ "$unamestr" == 'Linux' ]]; then
   platform='linux'
elif [[ "$unamestr" == 'Darwin' ]]; then
   platform='osx'
fi

if [[ $platform == 'linux' ]]; then
   fullpath=`readlink -f $0`
elif [[ $platform == 'osx' ]]; then
   TARGET_FILE=$0
   cd `dirname $TARGET_FILE`
   TARGET_FILE=`basename $TARGET_FILE`

   # Iterate down a (possible) chain of symlinks
   while [ -L "$TARGET_FILE" ]
   do
       TARGET_FILE=`readlink $TARGET_FILE`
       cd `dirname $TARGET_FILE`
       TARGET_FILE=`basename $TARGET_FILE`
   done

   # Compute the canonicalized name by finding the physical path
   # for the directory we're in and appending the target file.
   PHYS_DIR=`pwd -P`
   RESULT=$PHYS_DIR/$TARGET_FILE
   fullpath=$RESULT
fi

basedir=`dirname ${fullpath}`

########################################
# Now add to classpath the OSGi JAR
########################################
CLASSPATH=${basedir}/lib/org.eclipse.osgi-3.8.1.v20120830-144521.jar
FWCLASSPATH=file:${basedir}/lib/org.eclipse.osgi-3.8.1.v20120830-144521.jar

########################################
# Now add the extensions
########################################

# Extension 1: this is used to be able to convert all the
# bundleresouce: URL in file: so packages that are not OSGi ready can
# still work. Notably this is the case for spring classes
CLASSPATH=${CLASSPATH}:${basedir}/lib/org.eclipse.virgo.kernel.equinox.extensions-3.6.0.RELEASE.jar
FWCLASSPATH=${FWCLASSPATH},file:${basedir}/lib/org.eclipse.virgo.kernel.equinox.extensions-3.6.0.RELEASE.jar

########################################
# Now add the launcher
########################################
CLASSPATH=${CLASSPATH}:${basedir}/lib/org.eclipse.equinox.launcher-1.3.0.v20120522-1813.jar
FWCLASSPATH=${FWCLASSPATH},file:${basedir}/lib/org.eclipse.equinox.launcher-1.3.0.v20120522-1813.jar

$JAVA_HOME/bin/java $@ \
    -Djava.io.tmpdir=${basedir}/work/tmp \
    -Dosgi.install.area=${basedir} \
    -Dosgi.configuration.area=${basedir}/configuration \
    -Dosgi.frameworkClassPath=${FWCLASSPATH} \
    -Dosgi.framework=file:${basedir}/lib/org.eclipse.osgi-3.8.1.v20120830-144521.jar \
    -classpath ${CLASSPATH} \
    org.eclipse.equinox.launcher.Main \
    -console \
    -consoleLog
