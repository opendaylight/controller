//constructs a table of mount points via a json response
//Loads the table into the given dom.
var loadMountList = function( dom ) {
    dom.empty();
    dom.append( "<p>Loading. Please wait...</p>" );
    $.ajax( {
        url: "/apidoc/apis/mounts",
        datatype: 'jsonp',
        success: function( strData ){
            var myData = strData;
            var list = $( "<table></table>" );
            for( var key in myData )
            {
                list.append( "<tr><td><a href=\"#\" onclick=\"loadMount(" + 
                			 myData[key].id + ", '" + myData[key].instance + "')\">" +
                			 myData[key].instance + "</a></td></tr>");
            }
            dom.empty();
            dom.append( list );
        }
    } );
}