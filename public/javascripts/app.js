var GLOBALS = {
    buildRaters: function(selector, context) {
        $(selector, context).raty({
            half: true,
            click: function(score, evt) {
                var $movie = $(this).parents(".movie");

                jsRoutes.controllers.MovieController.rate($movie.attr("id"), score).ajax({
                    success: function(data) {
                        $("#movie-mask-ok").clone().removeAttr("id").appendTo($movie).fadeIn("slow", function(){
                            $(this).fadeOut("slow", function() { $(this).remove() });
                        });
                    }
                });
            }
        }).find("img").tooltip({container: "body"});

    }
}

$(function(){
    $.fn.raty.defaults.path = '/assets/images/icons';

    GLOBALS.buildRaters(".rate", document);


});

