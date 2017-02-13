app.service('authInterceptor', function($q, $rootScope) {
  var service = this

  service.responseError = function(response) {
    if (response.status == 401) {
      $rootScope.$broadcast('api.unauthorized')
    }

    return $q.reject(response)
  }
})

app.service('loginService', function($rootScope, $cookies, $http, state) {
  var service = this

  service.userInRole = function(/*str*/ role) {
    return state.userProfile != null && state.userProfile.roles.indexOf(role) != -1
  }

  service.setHttpAuthHeader = function() {
    service.removeHttpAuthHeader()
    $http.defaults.headers.common['Authorization'] = 'Bearer '+state.accessToken
  }

  service.removeHttpAuthHeader = function() {
     delete $http.defaults.headers.common['Authorization']
  }

  service.login = function(/*json*/ credentials) {
    $http.post('/api/auth/token', JSON.stringify(credentials))
      .then(
        function successCallback(response) {

          state.accessToken = response.data.access_token
          $cookies.put('token', state.accessToken)
          service.setHttpAuthHeader()
          $rootScope.$broadcast('api.authorized')

        })
  }

  service.refreshUser = function() {
    $http.get('/api/profiles/me')
      .then(
        function successCallback(response) {

          state.userProfile = response.data
          $rootScope.userReady = true
          $rootScope.$broadcast('api.user.ok')

        })
  }

})

app.service('modalDialogService', function($timeout) {
  var service = this

  var dialogs = {}

  service.registerDialog = function(/*selector*/ selector) {
    dialogs[selector] = {
      hidden: true
    }

    $(selector).on('hidden.bs.modal', function() {
      dialogs[selector].hidden = true
    })
    $(selector).on('show.bs.modal', function() {
      dialogs[selector].hidden = false
    })

    return {
      show: function() {
        service.showDialog(selector)
      }
    }
  }

  service.showDialog = function(/*selector*/ selector) {
    var f = function() {
      $(selector).modal({
        backdrop: 'static'
      })
    }
    if (dialogs[selector].hidden) f()
    else $timeout(f, 500)
  }

})

app.service('notifyService', function($timeout) {
  var service = this

  var defaultOptions = {
  }

  var defaultSettings = {
    newest_on_top: true,
    placement: {
      align: 'center'
    }
  }

  service.notify = function(/*json|str*/ options, /*json|str*/ settings) {
    options = $.extend(true, {}, defaultOptions, typeof options === 'object' ? options : { message: options })
    settings = $.extend(true, {}, defaultSettings, typeof settings === 'object' ? settings : { type: settings })
    $.notify(options, settings)
  }

  service.featureNotImplemented = function() {
    service.notify('This feature is not implemented!', 'danger')
  }

})
