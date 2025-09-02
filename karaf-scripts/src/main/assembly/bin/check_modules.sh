#!/bin/sh
#
# Copyright (c) 2019 Lumina Networks, Inc. and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
#
# This script checks the YANG modules collected by the extract_modules.sh
# script. The check is done by the pyang tool and an error text file is
# generated for every opendaylight project so proper actions can be taken
# to fix the modules.
#

BIN_DIR=`dirname $0`
OUTPUT="$BIN_DIR/../opendaylight-models"

# Exit if yang module folder is not found
[ ! -d "$OUTPUT" ] && echo "ERROR: no modules found, run extract_module.sh first" && exit 1

# Assemble the pyang command options
flags=""
PROJECTS=`ls -d $OUTPUT/*`
for proj in $PROJECTS; do
    proj=`basename $proj`
    flags="$flags -p  $OUTPUT/$proj"
done

# Run pyang on opendaylight yang modules and generate error file per project
for proj in $PROJECTS; do
    proj=`basename $proj`
    [ -f "$OUTPUT/$proj/errors.txt" ] && rm $OUTPUT/$proj/errors.txt
    touch $OUTPUT/$proj/errors.txt
    YANGS=`find $OUTPUT/$proj -type f -name '*.yang'`
    for yang in $YANGS; do
        less $yang | grep -q namespace.*opendaylight &&
        echo $yang && pyang $flags $yang 2>> $OUTPUT/$proj/errors.txt
    done
    echo "Write error file: $OUTPUT/$proj/errors.txt"
done
