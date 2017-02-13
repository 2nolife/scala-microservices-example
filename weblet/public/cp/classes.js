function Bookmark(/*json*/ source, /*services*/ sc) {

  var _this = this

  this.source = source

  function applyChangesFromSource() {
    _this.id = source.bookmark_id
    _this.url = source.url
    _this.rating = source.rating
    _this.owner = source._owner
  }

  applyChangesFromSource()

  /** get bookmark owner from API (expands the source object) */
  this.refreshOwner = function(/*fn*/ callback) {
    if (!_this.owner) {

      sc.apiUsersService.getUser(source.profile_id,
        function(/*User*/ user) {
          _this.owner = user
          source._owner = _this.owner
          callback()
        })

    }
  }

  this.refreshThis = function(/*fn*/ callback) {
    sc.apiBookmarksService.getBookmark(_this.id,
      function(/*Bookmark*/ bookmark) {
        _this.copyFrom(bookmark)
        callback()
      })
  }

  this.copyFrom = function(/*json|Bookmark*/ src) {
    var json = src.source ? src.source : src
    replaceInternals(source, json)
    applyChangesFromSource()
  }

}

function User(/*json*/ source, /*services*/ sc) {

  this.source = source

  this.id = source.profile_id
  this.username = source.username
  this.email = source.email

}
