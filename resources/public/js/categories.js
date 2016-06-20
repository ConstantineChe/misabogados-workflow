$(document).ready(function() {
    var input = $('#categories-filter');
    var button = $('#category-search');

    $.ajax({
        url: "/categories_json",
    }).done(function(data) {
        input.typeahead({source: data,
                         autoSelect: true});
    });



    input.change(function() {
        var current = input.typeahead("getActive");
        if (current) {
            // Some item from your model is active!
            if (current.name == input.val()) {
                window.location = "categoria/" + input.typeahead("getActive").id;
                // This means the exact match is found. Use toLowerCase() if you want case insensitive match.
            } else {
                // This means it is only a partial match, you can either add a new item
                // or take the active if you don't want new items
            }
        } else {
            // Nothing is active so it is a new value (or maybe empty value)
        }
    });

    input[0].addEventListener("keydown", function(ev) {
        if (ev.keyCode === 13) {
            window.location = "categoria/" + input.typeahead("getActive").id;
        }
    });

    button[0].addEventListener("click", function(ev) {
        window.location = "categoria/" + input.typeahead("getActive").id;
    });




});
