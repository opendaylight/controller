#!/bin/bash

##HELP
# For more information on a specific command, type -help command-name.
#
#   jmx              [-jmx]
#   jmxport          [-jmxport <num>] - DEFAULT is 1088
#   debug            [-debug]
#   debugsuspend     [-debugsuspend]
#   debugport        [-debugport <num>] - DEFAULT is 8000
#   start            [-start [<console port>]] - DEFAULT port is 2400
#   stop             [-stop]
#   status           [-status]
#   console          [-console]
#   agentpath        [-agentpath:<path to lib>]
##

platform='unknown'
unamestr=`uname`
if [[ "$unamestr" == 'Linux' ]]; then
   platform='linux'
elif [[ "$unamestr" == 'Darwin' ]]; then
   platform='osx'
fi

if [[ $platform == 'linux' ]]; then
   fullpath=`readlink -f $0`

   if [[ -z ${JAVA_HOME} ]]; then
      # Find the actual location of the Java launcher:
      java_launcher=`command -v java`
      java_launcher=`readlink -f "${java_launcher}"`

      # Compute the Java home from the location of the Java launcher:
      export JAVA_HOME="${java_launcher%/bin/java}"
    fi
elif [[ $platform == 'osx' ]]; then
   TARGET_FILE=$0
   cd `dirname "$TARGET_FILE"`
   TARGET_FILE=`basename $TARGET_FILE`

   # Iterate down a (possible) chain of symlinks
   while [ -L "$TARGET_FILE" ]
   do
       TARGET_FILE=`readlink "$TARGET_FILE"`
       cd `dirname "$TARGET_FILE"`
       TARGET_FILE=`basename "$TARGET_FILE"`
   done

   # Compute the canonicalized name by finding the physical path
   # for the directory we're in and appending the target file.
   PHYS_DIR=`pwd -P`
   RESULT=$PHYS_DIR/$TARGET_FILE
   fullpath=$RESULT

   [[ -z ${JAVA_HOME} ]] && [[ -x "/usr/libexec/java_home" ]] && export JAVA_HOME=`/usr/libexec/java_home -v 1.7`;

fi

[[ -z ${JAVA_HOME} ]] && echo "Need to set JAVA_HOME environment variable" && exit -1;
[[ ! -x ${JAVA_HOME}/bin/java ]] && echo "Cannot find an executable \
JVM at path ${JAVA_HOME}/bin/java check your JAVA_HOME" && exit -1;

if [ -z ${ODL_BASEDIR} ]; then
    basedir=`dirname "${fullpath}"`
else
    basedir=${ODL_BASEDIR}
fi

if [ -z ${ODL_DATADIR} ]; then
    datadir=`dirname "${fullpath}"`
else
    datadir=${ODL_DATADIR}
fi

if [ -z ${TMP} ]; then
    pidfile="/tmp/opendaylight.PID"
else
    pidfile="${TMP}/opendaylight.PID"
fi
debug=0
debugsuspend=0
debugport=8000
debugportread=""
startdaemon=0
daemonport=2400
daemonportread=""
jmxport=1088
jmxportread=""
startjmx=0
stopdaemon=0
statusdaemon=0
consolestart=1
dohelp=0
jvmMaxMemory="-Xmx1G"
extraJVMOpts=""
agentPath=""
unknown_option=0
helper=""
while true ; do
    case "$1" in
        -debug) debug=1; shift ;;
        -help) dohelp=1; shift;  helper=$1; break ;;
        -jmx) startjmx=1; shift ;;
        -debugsuspend) debugsuspend=1; shift ;;
        -debugport) shift; debugportread="$1"; if [[ "${debugportread}" =~ ^[0-9]+$ ]] ; then debugport=${debugportread}; shift; else echo "-debugport expects a number but was not found"; exit -1; fi;;
        -jmxport) shift; jmxportread="$1"; if [[ "${jmxportread}" =~ ^[0-9]+$ ]] ; then jmxport=${jmxportread}; shift; else echo "-jmxport expects a number but was not found"; exit -1; fi;;
        -start) startdaemon=1; shift; daemonportread="$1"; if [[ "${daemonportread}" =~ ^[0-9]+$ ]] ; then daemonport=${daemonportread}; shift; fi;;
        -stop) stopdaemon=1; shift ;;
        -status) statusdaemon=1; shift ;;
        -console) shift ;;
        -Xmx*) jvmMaxMemory="$1"; shift;;
        -D*) extraJVMOpts="${extraJVMOpts} $1"; shift;;
        -X*) extraJVMOpts="${extraJVMOpts} $1"; shift;;
        -agentpath:*) agentPath="$1"; shift;;
        "") break ;;
        *) echo "Unknown option $1"; unknown_option=1; break ;;
    esac
done



if [ "${unknown_option}" -eq 1 ]; then
    echo "Use -help for more information."
    exit 1
fi


if [ "${dohelp}" -eq 1 ]; then
    . ${basedir}/functions.sh
    harvestHelp ${helper}
    echo -e '\nFor other information type -help.\n'
    exit 1
fi

extraJVMOpts="${extraJVMOpts} ${jvmMaxMemory}"

##debugport
#debugport        [-debugport <num>] - DEFAULT is 8000
#
#    Set address for settings in runjdwp in extra JVM options.
#    The address is transport address for the connection.
#    The address has to be in the range [1024,65535]. If this option was not call, port will be set to default value.
##
# Validate debug port
if [[ "${debugport}" -lt 1024 ]] || [[ "${debugport}" -gt 65535 ]]; then
    echo "Debug Port not in the range [1024,65535] ${debugport}"
    exit -1
fi

# Validate daemon console port
if [[ "${daemonport}" -lt 1024 ]] || [[ "${daemonport}" -gt 65535 ]]; then
    echo "Daemon console Port not in the range [1024,65535] value is ${daemonport}"
    exit -1
fi

##jmxport
#jmxport          [-jmxport <num>] - DEFAULT is 1088
#
#    Set jmx port for com.sun.management.jmxremote.port in JMX support. Port has to be in the range [1024,65535]. If this option was not call, port will be set to default value.
##
# Validate jmx port
if [[ "${jmxport}" -lt 1024 ]] || [[ "${jmxport}" -gt 65535 ]]; then
    echo "JMX Port not in the range [1024,65535] value is ${jmxport}"
    exit -1
fi
##debug
#debug            [-debug]
#
#Run ODL controller with -Xdebug and -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=\${debugport}
#-Xdebug enables debugging capabilities in the JVM which are used by the Java Virtual Machine Tools Interface (JVMTI). JVMTI is a low-level debugging interface used by debuggers and profiling tools.
#-Xrunjdwp option loads the JPDA reference implementation of JDWP. This library resides in the target VM and uses JVMDI and JNI to interact with it. It uses a transport and the JDWP protocol to
#communicate with a separate debugger application.
#settings for -Xrunjdwp:
#            transport -  name of the transport to use in connecting to debugger application
#            server    -  if “y”, listen for a debugger application to attach; otherwise, attach to the debugger application at the specified address
#                      -  if “y” and no address is specified, choose a transport address at which to listen for a debugger application, and print the address to the standard output stream
#            suspend   -  if “y”, VMStartEvent has a suspend Policy of SUSPEND_ALL
#                      -  if “n”, VMStartEvent has a suspend policy of SUSPEND_NONE
#            address   -  transport address for the connection
#                      -  if server=n, attempt to attach to debugger application at this address
#          -  if server=y, listen for a connection at this address
##

##debugsuspend
#debugsuspend     [-debugsuspend]
#
#This command sets suspend on true in runjdwp in extra JVM options. If its true, VMStartEvent has a suspendPolicy of SUSPEND_ALL. If its false, VMStartEvent has a suspendPolicy of SUSPEND_NONE.
##
# Debug options
if [ "${debugsuspend}" -eq 1 ]; then
    extraJVMOpts="${extraJVMOpts} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=${debugport}"
elif [ "${debug}" -eq 1 ]; then
    extraJVMOpts="${extraJVMOpts} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${debugport}"
fi
##jmx
#jmx              [-jmx]
#
#Add JMX support. With settings for extra JVM options: -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=\${jmxport} -Dcom.sun.management.jmxremote
#jmxport can by set with command -jmxport <num>. Default num for the option is 1088.
##
# Add JMX support
if [ "${startjmx}" -eq 1 ]; then
    extraJVMOpts="${extraJVMOpts} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=${jmxport} -Dcom.sun.management.jmxremote"
fi

########################################
# Now add to classpath the OSGi JAR
########################################
CLASSPATH=${CLASSPATH}:${basedir}/lib/org.eclipse.osgi-3.8.1.v20120830-144521.jar
FWCLASSPATH=file:"${basedir}"/lib/org.eclipse.osgi-3.8.1.v20120830-144521.jar

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

cd $basedir

##stop
#stop             [-stop]
#
#If a controller is running, the command stop controller. Pid will be clean.
##
if [ "${stopdaemon}" -eq 1 ]; then
    if [ -e "${pidfile}" ]; then
        daemonpid=`cat "${pidfile}"`
        kill "${daemonpid}"
        rm -f "${pidfile}"
        echo "Controller with PID: ${daemonpid} -- Stopped!"
        exit 0
    else
        echo "Doesn't seem any Controller daemon is currently running"
        exit -1
    fi
fi

##status
#status           [-status]
#
#Find out whether a controller is running and print it.
##
if [ "${statusdaemon}" -eq 1 ]; then
    if [ -e "${pidfile}" ]; then
        daemonpid=`cat "${pidfile}"`
        ps -p ${daemonpid} > /dev/null
        daemonexists=$?
        if [ "${daemonexists}" -eq 0 ]; then
            echo "Controller with PID: ${daemonpid} -- Running!"
            exit 0
        else
            echo "Controller with PID: ${daemonpid} -- Doesn't seem to exist"
            rm -f "${pidfile}"
            exit 1
        fi
    else
        echo "Doesn't seem any Controller daemon is currently running, at least no PID file has been found"
        exit -1
    fi
fi

iotmpdir=`echo "${datadir}" | sed 's/ /\\ /g'`
bdir=`echo "${basedir}" | sed 's/ /\\ /g'`
confarea=`echo "${datadir}" | sed 's/ /\\ /g'`
fwclasspath=`echo "${FWCLASSPATH}" | sed 's/ /\\ /g'`

##start
#start            [-start [<console port>]]
#
#    If controller is not running, the command with argument(for set port, where controller has start) will start new controller on a port. The port has to be in the range [1024,65535]. If this option was not call, port will be set to default value. Pid will be create.
##
##console
#console          [-console]
#
#    Default option.
##
##agentpath
#agentpath        [-agentpath:<path to lib>]
#
#   Agentpath option passes path to agent to jvm in order to load native agent library, e.g. yourkit profiler agent.
##
echo "JVM maximum memory was set to ${jvmMaxMemory}."
if [ "${startdaemon}" -eq 1 ]; then
    if [ -e "${pidfile}" ]; then
        echo "Another instance of controller running, check with $0 -status"
        exit -1
    fi
    $JAVA_HOME/bin/java ${extraJVMOpts} \
        ${agentPath} \
        -Djava.io.tmpdir="${iotmpdir}/work/tmp" \
        -Dosgi.install.area="${bdir}" \
        -Dosgi.configuration.area="${confarea}/configuration" \
        -Dosgi.frameworkClassPath="${fwclasspath}" \
        -Dosgi.framework=file:"${bdir}/lib/org.eclipse.osgi-3.8.1.v20120830-144521.jar" \
        -Djava.awt.headless=true \
        -classpath "${CLASSPATH}" \
        org.eclipse.equinox.launcher.Main \
        -console ${daemonport} \
        -consoleLog &
    daemonpid=$!
    echo ${daemonpid} > ${pidfile}
elif [ "${consolestart}" -eq 1 ]; then
    if [ -e "${pidfile}" ]; then
        echo "Another instance of controller running, check with $0 -status"
        exit -1
    fi
    $JAVA_HOME/bin/java ${extraJVMOpts} \
        ${agentPath} \
        -Djava.io.tmpdir="${iotmpdir}/work/tmp" \
        -Dosgi.install.area="${bdir}" \
        -Dosgi.configuration.area="${confarea}/configuration" \
        -Dosgi.frameworkClassPath="${fwclasspath}" \
        -Dosgi.framework=file:"${bdir}/lib/org.eclipse.osgi-3.8.1.v20120830-144521.jar" \
        -Djava.awt.headless=true \
        -classpath "${CLASSPATH}" \
        org.eclipse.equinox.launcher.Main \
        -console \
        -consoleLog
fi
