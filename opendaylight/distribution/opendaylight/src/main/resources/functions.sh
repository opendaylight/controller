#!/bin/bash

# Function harvestHelp searches in run.sh part for line starting with "##<name command>".
# Next lines starting with "#<text>" will be printed without first char # (=help content).
# Help content has to end with "##" on new line.
# Example:
##foo
#   Foo is world wide used synnonym for bar.
##
function harvestHelp() {
    key="$1"
    if [ -z "${key}" ]; then
         key='HELP'
    fi
    echo
    sed -rn "/^##${key}$/,/^##/ p" $0 | sed -r '1 d; $ d; s/^#/  /'
    grep "##${key}" $0 > /dev/null
}
