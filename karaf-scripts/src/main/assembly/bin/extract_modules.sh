#!/bin/sh
#
# Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
# Copyright (c) 2019 Lumina Networks, Inc. and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
#
# This script visits all jars within the OpenDaylight karaf distribution and
# extracts all production YANG modules (as located in META-INF/yang).
#

BIN_DIR=`dirname $0`
OUTPUT="$BIN_DIR/../opendaylight-models"
INPUT="$BIN_DIR/.."

# Create folder for external yang modules
[ -d "$OUTPUT" ] && rm -rf $OUTPUT
mkdir $OUTPUT
mkdir $OUTPUT/external

PROJECTS=`ls -d $INPUT/system/org/opendaylight/*`
for proj in $PROJECTS; do
    proj=`basename $proj`
    # Create folder for project yang modules
    [ -d "$OUTPUT/$proj" ] || mkdir $OUTPUT/$proj
    # Extract yang modules from jars
    echo "Extracting yang modules from $proj"
    JARS=`find $INPUT/system/org/opendaylight/$proj -type f -name '*.jar' | sort -u`
    for jar in $JARS; do
        unzip -l "$jar" | grep -q -e "\.yang$" &&
        unzip -q "$jar" 'META-INF/yang/*' -d "$OUTPUT/$proj"
    done
    # Remove folder if no yang modules found
    if [ -z "$(ls -A $OUTPUT/$proj)" ]; then
        rm -rf $OUTPUT/$proj
    # Copy yang modules to project or external folder
    else
        YANGS=`find $OUTPUT/$proj/META-INF/yang/ -type f -name '*.yang'`
        for yang in $YANGS; do
            name=`basename $yang`
            less $yang | grep -q namespace.*opendaylight &&
            cp $OUTPUT/$proj/META-INF/yang/$name $OUTPUT/$proj
            less $yang | grep -q namespace.*opendaylight ||
            cp $OUTPUT/$proj/META-INF/yang/$name $OUTPUT/external
            echo "  $name"
        done
        # Clean temp folders
        rm -rf $OUTPUT/$proj/META-INF
    fi
done
echo "Yang Modules are extracted to `readlink -f $OUTPUT`"
