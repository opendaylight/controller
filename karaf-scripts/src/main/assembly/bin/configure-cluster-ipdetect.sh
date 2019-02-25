#!/bin/bash
#
# Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
# Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
 This script is used to configure cluster parameters on this
 controller. The user should restart controller to apply changes.

 Usage: $0 <seed_nodes_list>
  - seed_nodes_list: List of seed nodes, separated by comma or space.

 The script checks that one (any) of the the controller's active IP
 addresses is present in the seed_nodes_list. When running this script
 on multiple  seed nodes, keep the seed_node_list same on all nodes.

 Optionally, shards can be configured in a more granular way by
 modifying the file "custom_shard_configs.txt" in the same folder
 as this tool.  Please see that file for more details.

This script is currently limited to IPv4 addresses. If you have
problems running this script, please use 'configure_cluster.sh'.

EOF

    exit 1
}


function start_banner
{
cat <<EOF
################################################
##             Configure Cluster              ##
################################################
EOF
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

# Utility function for joining strings.
function join {
    delim=',\n\t\t\t\t'
    final=$1; shift

    for str in $* ; do
        final=${final}${delim}${str}
    done

    echo ${final}
}

function create_strings
{
    # Using a list of controller IPs, create the strings for data
    # and rpc seed nodes, as well as the list of members.

    # First create an arrays with one string per controller.
    # Then merge the array using the join utility defined above.
    count=1
    for ip in ${CONTROLLERIPS[@]} ; do
        ds[$count]=\\\"akka.tcp:\\/\\/opendaylight-cluster-data@${ip}:2550\\\"
        rpc[$count]=\\\"akka.tcp:\\/\\/odl-cluster-rpc@${ip}:2551\\\"
        members[$count]=\\\"member-${count}\\\"
        count=$[count + 1]
    done

    DATA_SEED_LIST=$(join ${ds[@]})
    RPC_SEED_LIST=$(join ${rpc[@]})
    MEMBER_NAME_LIST=$(join ${members[@]})
}

function module_shards_builder
{

    module_shards_string="module-shards = [\n\t{\n\t\tname = \"default\"\n\t\tshards = [\n\t\t\t{\n\t\t\t\tname = \"default\"\n\t\t\t\treplicas = []\n\t\t\t}\n\t\t]\n\t}"
    for name in ${FRIENDLY_MODULE_NAMES[@]} ; do
        module_shards_string="${module_shards_string},\n\t{\n\t\tname = \"${name}\"\n\t\tshards = [\n\t\t\t{\n\t\t\t\tname=\"${name}\"\n\t\t\t\treplicas = []\n\t\t\t}\n\t\t]\n\t}"
    done

    echo -e ${module_shards_string}"\n]"
}

function modules_builder
{

    modules_string="modules = [\n\t"
    count=1
    for name in ${FRIENDLY_MODULE_NAMES[@]} ; do
        modules_string="${modules_string}\n\t{\n\t\tname = \"${name}\"\n\t\tnamespace = \"${MODULE_NAMESPACES[${count}]}\"\n\t\tshard-strategy = \"module\"\n\t},"
        count=$[count + 1]
    done

    # using ::-1 below to remove the extra comma we get from the above loop
    echo -e ${modules_string::-1}"\n]"
}


function get_index ()
{
    # Determine if the local IP address is in the CONTROLLER_LIST
    # and its index in the list. Return the index.

    local MY_IP=$1
    shift
    local IP_ADDRS=("$@")
    local COUNTER=1

    for IP in ${IP_ADDRS[@]} ;
    do
        if [ "$MY_IP" == "$IP" ]; then
            echo "$COUNTER"
            return
        fi
        COUNTER=$[$COUNTER + 1]
    done
    echo "$COUNTER"
}

function get_local_ip_addresses
{
    # Get the local node's IP addresses as list
    LOCAL_IPS=()
    for IP in `hostname -I`
    do
        LOCAL_IPS+=("$IP")
    done
    echo ${LOCAL_IPS[@]}
}

function get_cli_params
{
    # Check if params have been supplied
    CONTROLLER_LIST=$*

    # Verify we have controller list
    test "${CONTROLLER_LIST}" == "" && usage "Missing controller list"

    # Create the list of controllers from the CONTROLLER_LIST variable
    CONTROLLERIPS=( ${CONTROLLER_LIST//,/ } )

    # Get the local node's IP addresses
    LOCAL_IPS=$(get_local_ip_addresses)

    for CONTROLLER_IP in ${LOCAL_IPS[@]} ;
    do
        INDEX=$(get_index $CONTROLLER_IP ${CONTROLLERIPS[@]})
        if [ ${INDEX} -le ${#CONTROLLERIPS[@]} ] ; then
            break
        fi
    done

    test ${INDEX} -le 0 -o ${INDEX} -gt ${#CONTROLLERIPS[@]} && \
        usage "Controller's local IP address not in the controller list"

    CONTROLLER_ID="member-${INDEX}"
}


function modify_conf_files
{
    BIN_DIR=`dirname $0`
    CUSTOM_SHARD_CONFIG_FILE=${BIN_DIR}'/custom_shard_config.txt'
    echo "Configuring unique name in akka.conf"
    sed -i -e "/roles[ ]*=/ { :loop1 /.*\]/ b done1; N; b loop1; :done1 s/roles.*\]/roles = [\"${CONTROLLER_ID}\"]/}" ${AKKACONF}

    echo "Configuring hostname in akka.conf"
    sed -i -e "s/hostname[ ]*=.*\"/hostname = \"${CONTROLLER_IP}\"/" ${AKKACONF}

    echo "Configuring data and rpc seed nodes in akka.conf"
    sed -i -e "/seed-nodes[ ]*=/ { :loop2 /.*\]/ b done2; N; b loop2; :done2 s/seed-nodes.*opendaylight-cluster-data.*\]/seed-nodes = [${DATA_SEED_LIST}]/; s/seed-nodes.*odl-cluster-rpc.*\]/seed-nodes = [${RPC_SEED_LIST}]/}" ${AKKACONF}

    if [ -f ${CUSTOM_SHARD_CONFIG_FILE} ]; then
        source ${CUSTOM_SHARD_CONFIG_FILE}
        if [ "${#FRIENDLY_MODULE_NAMES[@]}" -ne "${#MODULE_NAMESPACES[@]}" ]; then
            echo -e "\ncustom shard config file \"${CUSTOM_SHARD_CONFIG_FILE}\" does not have the same number of FRIENDLY_MODULE_NAMES[] and MODULE_NAMESPACES[]\n"
            exit 1
        fi
        module_shards_builder > ${MODULESHARDSCONF}
        modules_builder > ${MODULESCONF}
        cat ${MODULESCONF}
    fi

    echo "Configuring replication type in module-shards.conf"
    sed -i -e "/^[^#].*replicas[ ]*=/ { :loop /.*\]/ b done; N; b loop; :done s/replicas.*\]/replicas = [${MEMBER_NAME_LIST}]/}" ${MODULESHARDSCONF}
}


function verify_configuration_files
{
    # Constants
    BIN_DIR=`dirname $0`
    test ${BIN_DIR} == '.' && BIN_DIR=${PWD}
    CONTROLLER_DIR=`dirname ${BIN_DIR}`
    CONF_DIR=${CONTROLLER_DIR}/configuration/initial
    AKKACONF=${CONF_DIR}/akka.conf
    MODULESCONF=${CONF_DIR}/modules.conf
    MODULESHARDSCONF=${CONF_DIR}/module-shards.conf

    # Verify configuration files are present in expected location.
    if [ ! -f ${AKKACONF} -o ! -f ${MODULESHARDSCONF} ]; then
        # Check if the configuration files exist in the system
        # directory, then copy them over.
        ORIG_CONF_DIR=${CONTROLLER_DIR}/system/org/opendaylight/controller/sal-clustering-config
        version=$(sed -n -e 's/.*<version>\(.*\)<\/version>/\1/p' ${ORIG_CONF_DIR}/maven-metadata-local.xml)
        ORIG_CONF_DIR=${ORIG_CONF_DIR}/${version}
        ORIG_AKKA_CONF=sal-clustering-config-${version}-akkaconf.xml
        ORIG_MODULES_CONF=sal-clustering-config-${version}-moduleconf.xml
        ORIG_MODULESHARDS_CONF=sal-clustering-config-${version}-moduleshardconf.xml

        if [ -f ${ORIG_CONF_DIR}/${ORIG_AKKA_CONF} -a \
             -f ${ORIG_CONF_DIR}/${ORIG_MODULES_CONF} -a \
             -f ${ORIG_CONF_DIR}/${ORIG_MODULESHARDS_CONF} ]; then
            cat <<EOF
 NOTE: Cluster configuration files not found. Copying from
 ${ORIG_CONF_DIR}
EOF
            mkdir -p ${CONF_DIR}
            cp ${ORIG_CONF_DIR}/${ORIG_AKKA_CONF} ${AKKACONF}
            cp ${ORIG_CONF_DIR}/${ORIG_MODULES_CONF} ${MODULESCONF}
            cp ${ORIG_CONF_DIR}/${ORIG_MODULESHARDS_CONF} ${MODULESHARDSCONF}

        else
            cat << EOF
 ERROR: Cluster configurations files not found. Please\
 configure clustering feature.
EOF
            exit 1
        fi
    fi
}

function main
{
    get_cli_params $*
    start_banner
    verify_configuration_files
    create_strings
    modify_conf_files
    end_banner
}

main $*

# vim: ts=4 sw=4 sts=4 et ft=sh :
