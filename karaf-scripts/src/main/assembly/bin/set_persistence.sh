#!/bin/bash
#
# Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#


function usage()
{
    # Print any error messages
    test "$1" != "" && echo " ERROR: $1"

    # Print standard usage help
    cat << EOF
 This script is used to enable or disable the config datastore
 persistence. The default state is enabled. The user should
 restart controller to apply changes. The script can be used
 before starting controller for the first time.

 Usage: $0 <on/off>

EOF

    exit 1
}


function end_banner
{
cat <<EOF
################################################
##   NOTE: Manually restart controller to     ##
##         apply configuration.               ##
################################################
EOF
}


function get_cli_params
{
    # Check if params have been supplied
    test $# -eq 0 && usage

    # First param is on/off
    SWITCH="$1"

    # Verify we only have 1 param
    test $# -ne 1 && usage "Too many parameters"
}


function modify_conf_file
{
    if [ "${SWITCH}" == "off"  ]; then
        echo "disabling config datastore persistence"
        sed -i -e "s/^#persistent=true/persistent=false/" ${CLUSTERCONF}
    elif [ "${SWITCH}" == "on"  ]; then
        echo "enabling config datastore persistence"
        sed -i -e "s/^persistent=false/#persistent=true/" ${CLUSTERCONF}
    else
        usage "Allowed values are on/off"
    fi
}


function verify_configuration_file
{
    # Constants
    BIN_DIR=`dirname $0`
    test ${BIN_DIR} == '.' && BIN_DIR=${PWD}
    CONTROLLER_DIR=`dirname ${BIN_DIR}`
    CONF_DIR=${CONTROLLER_DIR}/etc
    CLUSTERCONF=${CONF_DIR}/org.opendaylight.controller.cluster.datastore.cfg

    # Verify configuration files are present in expected location.
    if [ ! -f ${CLUSTERCONF} ]; then
        # Check if the configuration files exist in the system
        # directory, then copy them over.
        ORIG_CONF_DIR=${CONTROLLER_DIR}/system/org/opendaylight/controller/sal-clustering-config
        version=$(sed -n -e 's/.*<version>\(.*\)<\/version>/\1/p' ${ORIG_CONF_DIR}/maven-metadata-local.xml)
        ORIG_CONF_DIR=${ORIG_CONF_DIR}/${version}
        ORIG_CLUSTER_CONF=sal-clustering-config-${version}-datastore.cfg

        if [ -f ${ORIG_CONF_DIR}/${ORIG_CLUSTER_CONF} ]; then
            cat <<EOF
 NOTE: Cluster configuration file not found. Copying from
 ${ORIG_CONF_DIR}
EOF
            cp ${ORIG_CONF_DIR}/${ORIG_CLUSTER_CONF} ${CLUSTERCONF}

        else
            usage "Cluster configuration file not found"
        fi
    fi
}

function main
{
    get_cli_params "$@"
    verify_configuration_file
    modify_conf_file
    end_banner
}

main "$@"

# vim: ts=4 sw=4 sts=4 et ft=sh :

