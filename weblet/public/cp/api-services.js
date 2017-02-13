app.service('apiBookmarksService', function($http, notifyService, apiClassWrap) {
  var service = this

  function notifyResponseError(/*obj*/ response) {
    notifyService.notify('<strong>'+response.status+'</strong>', 'danger')
  }

  service.findBookmarks = function(/*fn*/ callback) {
    $http.get('/api/bookmarks')
      .then(
        function successCallback(response) {
          var places = response.data
          callback(places.map(function(place) { return apiClassWrap.wrap(place, 'bookmark') }))
        },
        function errorCallback(response) {
          notifyResponseError(response)
        })
  }

  service.addBookmark = function(/*json*/ entity, /*fn*/ callback) {
    $http.post('/api/bookmarks', entity)
      .then(
        function successCallback(response) {
          var bookmark = response.data
          callback(apiClassWrap.wrap(bookmark, 'bookmark'))
        },
        function errorCallback(response) {
          notifyResponseError(response)
        })
  }

  service.patchBookmark = function(/*str*/ bookmarkId, /*json*/ entity, /*fn*/ callback) {
    $http.patch('/api/bookmarks/'+bookmarkId, entity)
      .then(
        function successCallback(response) {
          var bookmark = response.data
          callback(apiClassWrap.wrap(bookmark, 'bookmark'))
        },
        function errorCallback(response) {
          notifyResponseError(response)
        })
  }

  service.getBookmark = function(/*str*/ bookmarkId, /*fn*/ callback) {
    $http.get('/api/bookmarks/'+bookmarkId)
      .then(
        function successCallback(response) {
          var bookmark = response.data
          callback(apiClassWrap.wrap(bookmark, 'bookmark'))
        },
        function errorCallback(response) {
          notifyResponseError(response)
        })
  }

  service.deleteBookmark = function(/*str*/ bookmarkId, /*fn*/ callback) {
    $http.delete('/api/bookmarks/'+bookmarkId)
      .then(
        function successCallback(response) {
          callback()
        },
        function errorCallback(response) {
          notifyResponseError(response)
        })
  }

})

app.service('apiUsersService', function($http, notifyService, apiClassWrap) {
  var service = this

  function notifyResponseError(/*obj*/ response) {
    notifyService.notify('<strong>'+response.status+'</strong>', 'danger')
  }

  service.getUser = function(/*str*/ userId, /*fn*/ callback) {
    $http.get('/api/profiles/'+userId)
      .then(
        function successCallback(response) {
          var user = response.data
          callback(apiClassWrap.wrap(user, 'user'))
        },
        function errorCallback(response) {
          notifyResponseError(response)
        })
  }

})

app.service('apiClassWrap', function($injector) {
  var service = this

  var sc = null

  function initServiceContainer() {
    if (!sc) sc = {
      apiUsersService: $injector.get('apiUsersService'),
      apiBookmarksService: $injector.get('apiBookmarksService')
    }
  }

  service.wrap = function(/*json*/ json, /*str*/ className) {
    initServiceContainer()

    var clz
    switch (className) {
      case 'user':
        clz = new User(json, sc)
        break
      case 'bookmark':
        clz = new Bookmark(json, sc)
        break
    }

    return clz
  }

})
