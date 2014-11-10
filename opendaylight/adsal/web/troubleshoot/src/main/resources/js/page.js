/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

//PAGE troubleshoot
one.f = {};

// specify dashlets and layouts
one.f.dashlet = {
    existingNodes : {
        id : 'existingNodes',
        name : 'Existing Nodes'
    },
    uptime: {
        id: 'uptime',
        name: 'Uptime'
    },
    flowsOrPorts: {
        id: "flowsOrPorts",
        name: "Statistics"
    }
};

one.f.menu = {
    left : {
        top : [
            one.f.dashlet.existingNodes
        ],
        bottom : [
            one.f.dashlet.uptime
        ]
    },
    right : {
        top : [],
        bottom : [
            one.f.dashlet.flowsOrPorts
        ]
    }
};

/** INIT **/
// populate nav tabs
$(one.f.menu.left.top).each(function(index, value) {
    var $nav = $(".nav", "#left-top");
    one.main.page.dashlet($nav, value);
});

$(one.f.menu.left.bottom).each(function(index, value) {
    var $nav = $(".nav", "#left-bottom");
    one.main.page.dashlet($nav, value);
});

$(one.f.menu.right.bottom).each(function(index, value) {
    var $nav = $(".nav", "#right-bottom");
    one.main.page.dashlet($nav, value);
});

/**Troubleshoot modules*/
one.f.troubleshooting = {
    rootUrl: "/controller/web/troubleshoot",
    rightBottomDashlet: {
        get: function() {
            var $rightBottomDashlet = $("#right-bottom").find(".dashlet");
            return $rightBottomDashlet;
        },
        setDashletHeader: function(label) {
            $("#right-bottom li a")[0].innerHTML = label;
        }
    },
    createTable: function(columnNames, body) {
        var tableAttributes = ["table-striped", "table-bordered", "table-condensed"];
        var $table = one.lib.dashlet.table.table(tableAttributes);
        var tableHeaders = columnNames;
        var $thead = one.lib.dashlet.table.header(tableHeaders);
        var $tbody = one.lib.dashlet.table.body(body, tableHeaders);
        $table.append($thead)
            .append($tbody);
        return $table;
    }
};

one.f.troubleshooting.existingNodes = {
        id: {
            popout: "one_f_troubleshooting_existingNodes_id_popout",
            modal: "one_f_troubleshooting_existingNodes_id_modal",
            existingNodesDataGrid: "one_f_troubleshooting_existingNodes_id_datagrid",
            portsDataGrid: "one_f_troubleshooting_existingNodes_id_portsDataGrid",
            flowsDataGrid: "one_f_troubleshooting_existingNodes_id_flowsDataGrid",
            refreshFlowsButton:"one_f_troubleshooting_existingNodes_id_refreshFlowsButton",
            refreshPortsButton:"one_f_troubleshooting_existingNodes_id_refreshPortsButton",
            modal : {
                nodeInfo : "one_f_troubleshooting_existingNodes_id_modal_nodeInfo",
                cancelButton : "one_f_troubleshooting_existingNodes_id_modal_cancelButton",
            }
        },
        load: {
            main: function($dashlet) {
                one.lib.dashlet.empty($dashlet);
                $dashlet.append(one.lib.dashlet.header(one.f.dashlet.existingNodes.name));
                // TODO(l): Add a generic auto expand function to one.lib and replace custom height setting.
                //$('#left-top').height('100%');
                one.f.troubleshooting.existingNodes.ajax(one.f.troubleshooting.rootUrl + "/existingNodes" , function(content) {
                    var $gridHTML = one.lib.dashlet.datagrid.init(one.f.troubleshooting.existingNodes.id.existingNodesDataGrid, {
                        searchable: true,
                        filterable: false,
                        pagination: true,
                        flexibleRowsPerPage: true
                        }, "table-striped table-condensed");
                    $dashlet.append($gridHTML);
                    var dataSource = one.f.troubleshooting.existingNodes.data.existingNodesGrid(content);
                    $("#" + one.f.troubleshooting.existingNodes.id.existingNodesDataGrid).datagrid({dataSource: dataSource});

                });
            },
            flows: function(nodeId) {
                try {
                    if(one.f.troubleshooting === undefined){
                        return;
                    }
                    $.getJSON(one.main.constants.address.prefix + "/troubleshoot/flowStats?nodeId=" + nodeId, function(content) {
                        $rightBottomDashlet = one.f.troubleshooting.rightBottomDashlet.get();
                        one.f.troubleshooting.rightBottomDashlet.setDashletHeader("Flows");
                        one.lib.dashlet.empty($rightBottomDashlet);
                        $rightBottomDashlet.append(one.lib.dashlet.header("Flow Details"));
                        var button = one.lib.dashlet.button.single("Refresh",
                                one.f.troubleshooting.existingNodes.id.refreshFlowsButton, "btn-primary", "btn-mini");
                        var $button = one.lib.dashlet.button.button(button);
                        $button.click(function() {
                            one.f.troubleshooting.existingNodes.load.flows(nodeId);
                        });
                        $rightBottomDashlet.append($button);
                        var $gridHTML = one.lib.dashlet.datagrid.init(one.f.troubleshooting.existingNodes.id.flowsDataGrid, {
                            searchable: true,
                            filterable: false,
                            pagination: true,
                            flexibleRowsPerPage: true
                            }, "table-striped table-condensed");
                        $rightBottomDashlet.append($gridHTML);
                        var dataSource = one.f.troubleshooting.existingNodes.data.flowsGrid(content);
                        $("#" + one.f.troubleshooting.existingNodes.id.flowsDataGrid).datagrid({dataSource: dataSource});
                    });
                } catch(e) {}
            },
            ports: function(nodeId) {
                try {
                    if(one.f.troubleshooting === undefined){
                        return;
                    }
                    $.getJSON(one.main.constants.address.prefix + "/troubleshoot/portStats?nodeId=" + nodeId, function(content) {
                        $rightBottomDashlet = one.f.troubleshooting.rightBottomDashlet.get();
                        one.f.troubleshooting.rightBottomDashlet.setDashletHeader("Ports");
                        one.lib.dashlet.empty($rightBottomDashlet);
                        $rightBottomDashlet.append(one.lib.dashlet.header("Port Details"));
                        var button = one.lib.dashlet.button.single("Refresh",
                                one.f.troubleshooting.existingNodes.id.refreshPortsButton, "btn-primary", "btn-mini");
                        var $button = one.lib.dashlet.button.button(button);
                        $button.click(function() {
                            one.f.troubleshooting.existingNodes.load.ports(nodeId);
                        });
                        $rightBottomDashlet.append($button);
                        var $gridHTML = one.lib.dashlet.datagrid.init(one.f.troubleshooting.existingNodes.id.portsDataGrid, {
                            searchable: true,
                            filterable: false,
                            pagination: true,
                            flexibleRowsPerPage: true
                            }, "table-striped table-condensed");
                        $rightBottomDashlet.append($gridHTML);
                        var dataSource = one.f.troubleshooting.existingNodes.data.portsGrid(content);
                        $("#" + one.f.troubleshooting.existingNodes.id.portsDataGrid).datagrid({dataSource: dataSource});
                    });
                } catch(e) {}
            }
        },
        ajax : function(url, callback) {
            $.getJSON(url, function(data) {
                callback(data);
            });
        },
        registry: {},
        modal : {
        },
        data : {
            existingNodesGrid: function(data) {
                var source = new StaticDataSource({
                    columns: [
                        {
                            property: 'nodeName',
                            label: 'Name',
                            sortable: true
                        },
                        {
                            property: 'nodeId',
                            label: 'Node ID',
                            sortable: true
                        },
                        {
                            property: 'statistics',
                            label: 'Statistics',
                            sortable: true
                        }
                    ],
                    data: data.nodeData,
                    formatter: function(items) {
                        $.each(items, function(index, item) {
                            item.nodeName = "<a href=\"javascript:one.f.troubleshooting.existingNodes.data.nodeInfo('"
                                + item.nodeId + "');\">" + item.nodeName + "</a>"
                            item["statistics"] = "<a href=\"javascript:one.f.troubleshooting.existingNodes.load.flows('" + item["nodeId"] + "');\">Flows</a>" +
                            " <a href=\"javascript:one.f.troubleshooting.existingNodes.load.ports('" + item["nodeId"] + "');\">Ports</a>";
                        });
                    },
                    delay: 0
                });
                return source;
            },
            portsGrid: function(data) {
                $.each(data.nodeData, function(index, item) {
                    item.rxPkts = one.lib.helper.parseInt(item.rxPkts);
                    item.txPkts = one.lib.helper.parseInt(item.txPkts);
                    item.rxBytes = one.lib.helper.parseInt(item.rxBytes);
                    item.txBytes = one.lib.helper.parseInt(item.txBytes);
                    item.rxDrops = one.lib.helper.parseInt(item.rxDrops);
                    item.txDrops = one.lib.helper.parseInt(item.txDrops);
                    item.rxErrors = one.lib.helper.parseInt(item.rxErrors);
                    item.txErrors = one.lib.helper.parseInt(item.txErrors);
                    item.rxFrameErrors = one.lib.helper.parseInt(item.rxFrameErrors);
                    item.rxOverRunErrors = one.lib.helper.parseInt(item.rxOverRunErrors);
                    item.rxCRCErrors = one.lib.helper.parseInt(item.rxCRCErrors);
                    item.collisions = one.lib.helper.parseInt(item.collisions);
                });
                var source = new StaticDataSource({
                    columns: [
                        {
                            property: 'nodeConnector',
                            label: 'Node Connector',
                            sortable: true
                        },
                        {
                            property: 'rxPkts',
                            label: 'Rx Pkts',
                            sortable: true
                        },
                        {
                            property: 'txPkts',
                            label: 'Tx Pkts',
                            sortable: true
                        },
                        {
                            property: 'rxBytes',
                            label: 'Rx Bytes',
                            sortable: true
                        },
                        {
                            property: 'txBytes',
                            label: 'Tx Bytes',
                            sortable: true
                        },
                        {
                            property: 'rxDrops',
                            label: 'Rx Drops',
                            sortable: true
                        },
                        {
                            property: 'txDrops',
                            label: 'Tx Drops',
                            sortable: true
                        },
                        {
                            property: 'rxErrors',
                            label: 'Rx Errs',
                            sortable: true
                        },
                        {
                            property: 'txErrors',
                            label: 'Tx Errs',
                            sortable: true
                        },
                        {
                            property: 'rxFrameErrors',
                            label: 'Rx Frame Errs',
                            sortable: true
                        },
                        {
                            property: 'rxOverRunErrors',
                            label: 'Rx OverRun Errs',
                            sortable: true
                        },
                        {
                            property: 'rxCRCErrors',
                            label: 'Rx CRC Errs',
                            sortable: true
                        },
                        {
                            property: 'collisions',
                            label: 'Collisions',
                            sortable: true
                        }
                    ],
                    data: data.nodeData,
                    delay: 0
                });
                return source;
            },
            ports: function(data) {
                var result = [];
                $.each(data.nodeData, function(key, value) {
                    var tr = {};
                    var entry = [];
                    entry.push(value["nodeConnector"]);
                    entry.push(value["rxPkts"]);
                    entry.push(value["txPkts"]);
                    entry.push(value["rxBytes"]);
                    entry.push(value["txBytes"]);
                    entry.push(value["rxDrops"]);
                    entry.push(value["txDrops"]);
                    entry.push(value["rxErrors"]);
                    entry.push(value["txErrors"]);
                    entry.push(value["rxFrameErrors"]);
                    entry.push(value["rxOverRunErrors"]);
                    entry.push(value["rxCRCErrors"]);
                    entry.push(value["collisions"]);
                    tr.entry = entry;
                    result.push(tr);
                });
                return result;
            },
            flowsGrid: function(data) {
                $.each(data.nodeData, function(index, item) {
                    item.byteCount = one.lib.helper.parseInt(item.byteCount);
                    item.packetCount = one.lib.helper.parseInt(item.packetCount);
                    item.durationSeconds = one.lib.helper.parseInt(item.durationSeconds);
                    item.idleTimeout = one.lib.helper.parseInt(item.idleTimeout);
                    item.priority = one.lib.helper.parseInt(item.priority);
                });
                var source = new StaticDataSource({
                    columns: [
                        {
                            property: 'nodeName',
                            label: 'Node',
                            sortable: true
                        },
                        {
                            property: 'inPort',
                            label: 'In Port',
                            sortable: true
                        },
                        {
                            property: 'dlSrc',
                            label: 'DL Src',
                            sortable: true
                        },
                        {
                            property: 'dlDst',
                            label: 'DL Dst',
                            sortable: true
                        },
                        {
                            property: 'dlType',
                            label: 'DL Type',
                            sortable: true
                        },
                        {
                            property: 'dlVlan',
                            label: 'DL Vlan',
                            sortable: true
                        },
                        {
                            property: 'dlVlanPriority',
                            label: 'Vlan PCP',
                            sortable: true
                        },
                        {
                            property: 'nwSrc',
                            label: 'NW Src',
                            sortable: true
                        },
                        {
                            property: 'nwDst',
                            label: 'NW Dst',
                            sortable: true
                        },
                        {
                            property: 'nwTOS',
                            label: 'ToS Bits',
                            sortable: true
                        },
                        {
                            property: 'nwProto',
                            label: 'NW Proto',
                            sortable: true
                        },
                        {
                            property: 'tpSrc',
                            label: 'TP Src',
                            sortable: true
                        },
                        {
                            property: 'tpDst',
                            label: 'TP Dst',
                            sortable: true
                        },
                        {
                            property: 'actions',
                            label: 'Actions',
                            sortable: true
                        },
                        {
                            property: 'byteCount',
                            label: 'Byte Count',
                            sortable: true
                        },
                        {
                            property: 'packetCount',
                            label: 'Packet Count',
                            sortable: true
                        },
                        {
                            property: 'durationSeconds',
                            label: 'Duration Seconds',
                            sortable: true
                        },
                        {
                            property: 'idleTimeout',
                            label: 'Idle Timeout',
                            sortable: true
                        },
                        {
                            property: 'priority',
                            label: 'Priority',
                            sortable: true
                        }
                    ],
                    data: data.nodeData,
                    delay: 0
                });
                return source;
            },
            flows: function(data) {
                var result = [];
                $.each(data.nodeData, function(key, value) {
                    var tr = {};
                    var entry = [];
                    entry.push(value["nodeName"]);
                    entry.push(value["inPort"]);
                    entry.push(value["dlSrc"]);
                    entry.push(value["dlDst"]);
                    entry.push(value["dlType"]);
                    entry.push(value["dlVlan"]);
                    entry.push(value["nwSrc"]);
                    entry.push(value["nwDst"]);
                    entry.push(value["nwTOS"]);
                    entry.push(value["nwProto"]);
                    entry.push(value["tpSrc"]);
                    entry.push(value["tpDst"]);
                    entry.push(value["actions"]);
                    entry.push(value["byteCount"]);
                    entry.push(value["packetCount"]);
                    entry.push(value["durationSeconds"]);
                    entry.push(value["idleTimeout"]);
                    entry.push(value["priority"]);
                    tr.entry = entry;
                    result.push(tr);
                });
                return result;
            },
            nodeInfo : function(nodeId){
                $.getJSON(one.main.constants.address.prefix + "/troubleshoot/nodeInfo?nodeId=" + nodeId, function(content) {
                    var h3 = 'Node Information'

                    var headers = [ 'Description','Specification'];

                    var attributes = ['table-striped', 'table-bordered', 'table-condensed'];
                    var $table = one.lib.dashlet.table.table(attributes);
                    var $thead = one.lib.dashlet.table.header(headers);
                    $table.append($thead);

                    var footer = [];

                    var cancelButton = one.lib.dashlet.button.single("Cancel",
                            one.f.troubleshooting.existingNodes.id.modal.nodeInfo, "", "");
                    var $cancelButton = one.lib.dashlet.button.button(cancelButton);
                    footer.push($cancelButton);

                    var body = []
                    $.each(content, function(key, value) {
                        var tr = {};
                        var entry = [];

                        entry.push(key);
                        entry.push(value);

                        tr.entry = entry;
                        body.push(tr);
                    });
                    var $tbody = one.lib.dashlet.table.body(body);
                    $table.append($tbody);

                    var $modal = one.lib.modal.spawn(one.f.troubleshooting.existingNodes.id.modal.nodeInfo, h3, $table , footer);
                    $modal.modal();

                    $('#'+one.f.troubleshooting.existingNodes.id.modal.nodeInfo, $modal).click(function() {
                        $modal.modal('hide');
                    });
                });
            }
        }
};

one.f.troubleshooting.uptime = {
    id: {
        popout: "one_f_troubleshooting_uptime_id_popout",
        modal: "one_f_troubleshooting_uptime_id_modal",
        datagrid: "one_f_troubleshooting_uptime_id_datagrid"
    },

    dashlet: function($dashlet) {
            one.lib.dashlet.empty($dashlet);
            $dashlet.append(one.lib.dashlet.header(one.f.dashlet.uptime.name));
            var url = one.f.troubleshooting.rootUrl + "/uptime";
            one.f.troubleshooting.uptime.ajax.main(url , {} ,function(content) {
                var $gridHTML = one.lib.dashlet.datagrid.init(one.f.troubleshooting.uptime.id.datagrid, {
                    searchable: true,
                    filterable: false,
                    pagination: true,
                    flexibleRowsPerPage: true
                    }, "table-striped table-condensed");
                $dashlet.append($gridHTML);
                var dataSource = one.f.troubleshooting.uptime.data.uptimeDataGrid(content);
                $("#" + one.f.troubleshooting.uptime.id.datagrid).datagrid({dataSource: dataSource});
            });
    },

    ajax : {
        main : function(url, requestData, callback) {
            $.getJSON(url, requestData, function(data) {
                callback(data);
            });
        }
    },

    data: {
        uptimeDataGrid: function(data) {
            var source = new StaticDataSource({
                columns: [
                    {
                        property: 'nodeName',
                        label: 'Node',
                        sortable: true
                    },
                    {
                        property: 'nodeId',
                        label: 'Node ID',
                        sortable: true
                    },
                    {
                        property: 'connectedSince',
                        label: 'Statistics',
                        sortable: true
                    }
                ],
                data: data.nodeData,
                delay: 0
            });
            return source;
        },
        uptime: function(data) {
            var result = [];
            $.each(data.nodeData, function(key, value) {
                var tr = {};
                var entry = [];
                entry.push(value["nodeName"]);
                entry.push(value["nodeId"]);
                entry.push(value["connectedSince"]);
                tr.entry = entry;
                result.push(tr);
            });
            return result;
        }
    },
};

one.f.troubleshooting.statistics = {
    dashlet : function($dashlet) {
        var $h4 = one.lib.dashlet.header("Statistics");
        $dashlet.append($h4);
        // empty
        var $none = $(document.createElement('div'));
        $none.addClass('none');
        var $p = $(document.createElement('p'));
        $p.text('Please select a Flow or Ports statistics');
        $p.addClass('text-center').addClass('text-info');

        $dashlet.append($none)
            .append($p);
    }
};

// bind dashlet nav
$('.dash .nav a', '#main').click(function() {
    // de/activation
    var $li = $(this).parent();
    var $ul = $li.parent();
    one.lib.nav.unfocus($ul);
    $li.addClass('active');
    // clear respective dashlet
    var $dashlet = $ul.parent().find('.dashlet');
    one.lib.dashlet.empty($dashlet);
    // callback based on menu
    var id = $(this).attr('id');
    var menu = one.f.dashlet;
    switch (id) {
        case menu.existingNodes.id:
            one.f.troubleshooting.existingNodes.load.main($dashlet);
            break;
        case menu.uptime.id:
            one.f.troubleshooting.uptime.dashlet($dashlet);
            break;
        case menu.flowsOrPorts.id:
            one.f.troubleshooting.statistics.dashlet($dashlet);
            break;
    };
});

// activate first tab on each dashlet
$('.dash .nav').each(function(index, value) {
    $($(value).find('li')[0]).find('a').click();
});
