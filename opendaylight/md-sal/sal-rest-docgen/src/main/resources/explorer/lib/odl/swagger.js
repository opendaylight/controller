
//There can only be a single swagger UI in a dom tree
//at a time - this is a limitation of the swagger API
//This method takes a URL which is the root of the swagger
//documentation, as well as the ID of the dom where we should
//load the swagger UI.
//See the swagger documentation for more information.
var loadSwagger = function(url, dom_id) {
    $("#" + dom_id).empty();
    $("#" + dom_id).append("Loading " + url);
    window.swaggerUi = new SwaggerUi({
        url : url,
        dom_id : dom_id,
        supportedSubmitMethods : [ 'get', 'post', 'put', 'delete' ],
        onComplete : function(swaggerApi, swaggerUi) {
            if (console) {
                console.log("Loaded SwaggerUI")
            }
            $('pre code').each(function(i, e) {
                hljs.highlightBlock(e)
            });
        },
        onFailure : function(data) {
            if (console) {
                console.log("Unable to Load SwaggerUI");
                console.log(data);
            }
        },
        docExpansion : "none"
    });

    $('#input_apiKey').change(
            function() {
                var key = $('#input_apiKey')[0].value;
                console.log("key: " + key);
                if (key && key.trim() != "") {
                    console.log("added key " + key);
                    window.authorizations
                            .add("key", new ApiKeyAuthorization("api_key",
                                    key, "query"));
                }
            });
    window.swaggerUi.load();
}