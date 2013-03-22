
/* 
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

//PAGE topology
one.f = {};

// specify dashlets and layouts
one.f.dashlet = {
    flows : {
        id : 'dashletFlows',
        name : 'Flows'
    },
    widget : {
        id : 'dashletWidget',
        name : 'Acme Widgets'
    },
    status : {
        id : 'dashletStatus',
        name : 'Network Status'
    },
    foo : {
        id : 'dashletFoo',
        name : 'Foo'
    },
    bar : {
        id : 'dashletBar',
        name : 'Bar'
    }
};

one.f.menu = {
    left : {
        top : [
            one.f.dashlet.flows,
            one.f.dashlet.bar
        ],
        bottom : [
            one.f.dashlet.foo
        ]
    },
    right : {
        top : [],
        bottom : [
            one.f.dashlet.widget,
            one.f.dashlet.status
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

one.f.populate = function($dashlet, header) {
    var $h4 = one.lib.dashlet.header(header);
    $dashlet.append($h4);
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
        case menu.foo.id:
            one.f.populate($dashlet, "Foo");
            break;
        case menu.flows.id:
            one.f.populate($dashlet, "Flows");
            break;
        case menu.bar.id:
            one.f.populate($dashlet, "Bar");
            break;
        case menu.widget.id:
            one.f.populate($dashlet, "Widget");
            break;
        case menu.status.id:
            one.f.populate($dashlet, "Status");
            break;
    };
});

// activate first tab on each dashlet
$('.dash .nav').each(function(index, value) {
    $($(value).find('li')[0]).find('a').click();
});
