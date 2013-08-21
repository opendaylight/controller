// global
var one = {
    // global variables
    global : {
        remoteAddress : "/"
    },
    role : null
}

// one ui library
one.lib = {};

// registry
one.lib.registry = {};

/** DASHLET */
one.lib.dashlet = {
    empty : function($dashlet) {
        $dashlet.empty();
    },
    append : function($dashlet, $content) {
        $dashlet.append($content);
    },
    header : function(header) {
        var $h4 = $(document.createElement('h4'));
        $h4.text(header);
        return $h4;
    },
    list : function(list) {
        var $ul = $(document.createElement('ul'));
        $(list).each(function(index, value) {
            var $li = $(document.createElement('li'));
            $li.append(value);
            $ul.append($li);
        });
        return $ul;
    },
    button : {
        config : function(name, id, type, size) {
            var button = {};
            button['name'] = name;
            button['id'] = id;
            button['type'] = type;
            button['size'] = size;
            return button;
        },
        single : function(name, id, type, size) {
            var buttonList = [];
            var button = one.lib.dashlet.button.config(name, id, type, size);
            buttonList.push(button);
            return buttonList;
        },
        button : function(buttonList) {
            var $buttonGroup = $(document.createElement('div'));
            $buttonGroup.addClass("btn-group");
            $(buttonList).each(function(index, value) {
                var $button = $(document.createElement('button'));
                $button.text(value.name);
                $button.addClass('btn');
                $button.addClass(value['type']);
                $button.addClass(value['size']);
                if (!(typeof value.id === 'undefined')) {
                    $button.attr('id', value.id);
                }
                $buttonGroup.append($button);
            });
            return $buttonGroup;
        }
    },
    datagrid: {
        /*
         * The init function returns HTML markup for the datagrid per the options provided. Each consumer 
         * of the datagrid must first call init and then provide the datasource for the grid.   
         * id: this is the id of the table
         * options: {
         * searchable: true/false,
         * pagination: turned off for now,
         * flexibleRowsPerPage: turned off
         * }
         * classes : String containing bootstrap related classes. For ex: "table-striped table-condensed"
         * The classes "table", "table-bordered" and "datagrid" will be added by default
         */
        init: function(id, options, classes) {
            var $fuelGridContainerDiv = $(document.createElement("div"));
            $fuelGridContainerDiv.addClass("fuelux");
            $table = $(document.createElement("table"));
            $table.attr("id", id);
            $table.addClass("table table-bordered datagrid");
            $table.addClass(classes);
            // create datagrid header
            $thead = $(document.createElement("thead"));
            $headertr = $(document.createElement("tr"));
            $headerth = $(document.createElement("th"));
            // create datagrid footer
            $tfoot = $(document.createElement("tfoot"));
            $footertr = $(document.createElement("tr"));
            $footerth = $(document.createElement("th"));
            if(options.searchable == true) {
                $headerth.append(one.lib.dashlet.datagrid._searchable());
            }
            if(options.flexibleRowsPerPage == true) {
                $footerth.append(one.lib.dashlet.datagrid._rowsPerPage(options.popout));
            }
            if(options.pagination == true) {
                $footerth.append(one.lib.dashlet.datagrid._pagination());
            }
            $headertr.append($headerth);
            $thead.append($headertr);
            $footertr.append($footerth);
            $tfoot.append($footertr);
            $table.append($thead).append($tfoot);
            $fuelGridContainerDiv.append($table);
            return $fuelGridContainerDiv;
        },
        _searchable: function() {
            var searchHTML = "<div class='datagrid-header-left'><div class='input-append search datagrid-search'> <input type='text' class='input-medium' placeholder='Search'><button type='button' class='btn'><i class='icon-search'></i></button></div></div>";
            return searchHTML;
        },
        _pagination: function() {
            var html = '<div class="datagrid-footer-right" style="display:none;"><div class="grid-pager"><button type="button" class="btn grid-prevpage"><i class="icon-chevron-left"></i></button><span>Page</span> <div style="display:inline-block;"><input type="text" name="pagenumber" style="width:25px;margin-bottom:-10px;vertical-align:middle;margin-right:5px;"></div><span>of <span class="grid-pages"></span></span><button type="button" class="btn grid-nextpage"><i class="icon-chevron-right"></i></button></div></div>';
            return html;
        },
        _rowsPerPage: function(popout) {
            if(popout) {
                var html = '<div class="datagrid-footer-left" style="display:none;"><div class="grid-controls"><span><span class="grid-start"></span>-<span class="grid-end"></span> of <span class="grid-count"></span></span><div class="select grid-pagesize" data-resize="auto" style="visibility:hidden;"><button type="button" data-toggle="dropdown" class="btn dropdown-toggle"><span class="dropdown-label"></span><span class="caret"></span></button><ul class="dropdown-menu"><li data-value="10" data-selected="true"><a href="#">5</a></li><li data-value="10"><a href="#">10</a></li><li data-value="20"><a href="#">20</a></li><li data-value="50"><a href="#">50</a></li><li data-value="100"><a href="#">100</a></li></ul></div><span style="display:none;">Per Page</span></div></div>';
            } else {
                var html = '<div class="datagrid-footer-left" style="display:none;"><div class="grid-controls"><span><span class="grid-start"></span>-<span class="grid-end"></span> of <span class="grid-count"></span></span><div class="select grid-pagesize" data-resize="auto" style="visibility:hidden;"><button type="button" data-toggle="dropdown" class="btn dropdown-toggle"><span class="dropdown-label"></span><span class="caret"></span></button><ul class="dropdown-menu"><li data-value="5" data-selected="true"><a href="#">5</a></li><li data-value="10"><a href="#">10</a></li><li data-value="20"><a href="#">20</a></li><li data-value="50"><a href="#">50</a></li><li data-value="100"><a href="#">100</a></li></ul></div><span style="display:none;">Per Page</span></div></div>';
            }
            return html;
        }
    },
    table : {
        table : function(classes, id) {
            var $table = $(document.createElement('table'));
            $table.addClass("table");
            $(classes).each(function(index, value) {
                $table.addClass(value);
            });
            if (!(typeof id === 'undefined'))
                $table.attr("id", id);
            return $table;
        },
        header : function(headers) {
            var $thead = $(document.createElement('thead'));
            var $tr = $(document.createElement('tr'));
            $(headers).each(function(index, value) {
                $th = $(document.createElement('th'));
                $th.append(value);
                $tr.append($th);
            });
            $thead.append($tr);
            return $thead;
        },
        body : function(body, thead) {
            var $tbody = $(document.createElement('tbody'));
            // if empty
            if (body.length == 0 && !(typeof thead === 'undefined')) {
                var $tr = $(document.createElement('tr'));
                var $td = $(document.createElement('td'));
                $td.attr('colspan', thead.length);
                $td.text('No data available');
                $td.addClass('empty');
                $tr.append($td);
                $tbody.append($tr);
                return $tbody;
            }
            // else, populate as usual
            $(body).each(function(index, value) {
                var $tr = $(document.createElement('tr'));
                $.each(value, function(key, value) {
                    if (key == 'type') {
                        // add classes
                        $(value).each(function(index, value) {
                            $tr.addClass(value);
                        });
                    } else if (key == 'entry') {
                        // add entries
                        $(value).each(function(index, value) {
                            var $td = $(document.createElement('td'));
                            $td.append(value);
                            $tr.append($td);
                        });
                    } else {
                        // data field
                        $tr.attr('data-' + key, value);
                    }
                    $tbody.append($tr);
                });
            });
            return $tbody;
        }
    },
    description : function(description, horizontal) {
        var $dl = $(document.createElement('dl'));
        if (horizontal == true) {
            $dl.addClass("dl-horizontal");
        }
        $(description).each(function(index, value) {
            var $dt = $(document.createElement('dt'));
            $dt.text(value.dt);
            var $dd = $(document.createElement('dd'));
            $dd.text(value.dd);
            $dl.append($dt).append($dd);
        });
        return $dl;
    }
}

/** MODAL */
one.lib.modal = {
    // clone default modal
    clone : function(id) {
        var $clone = $("#modal").clone(true);
        $clone.attr("id", id);
        return $clone;
    },
    // populate modal
    populate : function($modal, header, $body, footer) {
        var $h3 = $modal.find("h3");
        $h3.text(header);

        var $modalBody = $modal.find(".modal-body");
        $modalBody.append($body);

        $(footer).each(function(index, value) {
            $modal.find(".modal-footer").append(value);
        });
    },
    // clone and populate modal
    spawn : function(id, header, $body, footer) {
        var $modal = one.lib.modal.clone(id);
        one.lib.modal.populate($modal, header, $body, footer);
        return $modal;
    },
    // empty modal
    empty : function($modal) {
        $modal.find("h3").empty();
        $modal.find(".modal-body").empty();
        $modal.find(".modal-footer").empty();
    },
    // injection
    inject : {
        body : function($modal, $body) {
            $modal.find(".modal-body").empty();
            $modal.find(".modal-body").append($body);
        }
    }
}

/** FORM */
one.lib.form = {
    // create select-option form element
    select : {
        create : function(options, multiple) {
            // assert - auto assign
            if (options == undefined)
                options = {};

            var $select = $(document.createElement('select'));
            if (multiple == true) {
                $select.attr("multiple", "multiple");
            }
            var optionArray = one.lib.form.select.options(options);
            $(optionArray).each(function(index, value) {
                $select.append(value);
            });
            return $select;
        },
        options : function(options) {
            var result = [];
            $.each(options, function(key, value) {
                var $option = $(document.createElement('option'));
                $option.attr("value", key);
                $option.text(value);
                result.push($option);
            });
            return result;
        },
        empty : function($select) {
            $select.empty();
        },
        inject : function($select, options) {
            $select.empty();
            var options = one.lib.form.select.options(options);
            $select.append(options);
        },
        prepend : function($select, options) {
            var options = one.lib.form.select.options(options);
            $select.prepend(options);
        },
        bubble : function($select, bubble) {
            $($select.find("option")).each(function(index, value) {
                if ($(value).attr("value") == bubble) {
                    var option = $(value);
                    $(value).remove();
                    $select.prepend(option);
                    return;
                }
            });
        }
    },
    // create legend form element
    legend : function(name) {
        var $legend = $(document.createElement('legend'));
        $legend.text(name);
        return $legend;
    },
    // create label form element
    label : function(name) {
        var $label = $(document.createElement('label'));
        $label.text(name);
        return $label;
    },
    // create help block form element
    help : function(help) {
        var $help = $(document.createElement('span'));
        $help.text(help);
        $help.addClass("help-block");
        return $help;
    },
    // create generic text input
    input : function(placeholder) {
        var $input = $(document.createElement('input'));
        $input.attr('type', 'text');
        $input.attr('placeholder', placeholder);
        return $input;
    }
}

/** NAV */
one.lib.nav = {
    unfocus : function($nav) {
        $($nav.find("li")).each(function(index, value) {
            $(value).removeClass("active");
        });
    }
}

/** ALERT */
one.lib.alert = function(alert) {
    $("#alert p").text(alert);
    $("#alert").hide();
    $("#alert").slideToggle();
    clearTimeout(one.lib.registry.alert);
    one.lib.registry.alert = setTimeout(function() {
        $("#alert").slideUp();
    }, 8000);
}
