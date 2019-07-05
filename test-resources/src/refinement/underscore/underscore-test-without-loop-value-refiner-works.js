var objA = {};

var objB = {foo: function() {}};

var anyString = js_value_refiner_debug_top ? "foo" : "bar";

objA[anyString] = objB.foo;