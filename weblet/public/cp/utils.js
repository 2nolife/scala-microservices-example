function randomID(/*str*/ prefix, /*str*/ postfix) {
  return (prefix ? prefix : 'id_')+(''+Math.random()).substring(2)+(postfix ? postfix : '')
}

function randomScopeID() {
  return randomID('id_{{$id}}_')
}

function elementID(/*obj*/ element) {
  var id = element.attr('id')
  if (!id) {
    id = randomID()
    element.attr('id', id)
  }
  return id
}

function replaceInternals(/*obj*/ obj, /*obj*/ copyFrom) {
  Object.keys(obj).forEach(function(key) {
    delete obj[key]
  })
  $.extend(true, obj, copyFrom)
}

function assert(condition, message) {
  if (!condition) {
    message = message || "Assertion failed"
    if (typeof Error !== "undefined") throw new Error(message)
    throw message
  }
}

function safeParseInt(/*str*/ value) {
  return value ? parseInt(value) : null
}
