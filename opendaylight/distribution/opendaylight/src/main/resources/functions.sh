#!/bin/bash

function harvestHelp() {
    key="$1"
    if [ -z "${key}" ]; then
         key='HELP'
    fi
    echo
    sed -rn "/^##${key}$/,/^##/ p" $0 | sed -r '1 d; $ d; s/^#/  /'
    grep "##${key}" $0 > /dev/null
}
