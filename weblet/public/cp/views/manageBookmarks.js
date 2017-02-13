app.controller('manageBookmarksController', function($scope, apiBookmarksService, notifyService, $timeout, modalDialogService) {

  function NewBookmark() {

    var _this = this

    this.url = null
    this.rating = 0

    this.toApiBookmarkEntity = function() {
      return {
        url: _this.url,
        rating: _this.rating
      }
    }

  }

  function EditedBookmark(/*Bookmark*/ source) {

    var _this = this

    this.source = source

    this.id = source.id
    this.owner = source.owner
    this.url = source.url
    this.rating = source.rating

    source.refreshOwner(function() {
      _this.owner = source.owner
    })

    this.refresh = function(/*fn*/ callback) {
      source.refreshThis(function() {
        _this.url = source.url
        _this.rating = source.rating
        callback()
      })
    }

    this.toApiBookmarkEntity = function() {
      return {
        url: _this.url,
        rating: _this.rating
      }
    }

  }

  $scope.loadBookmarks = function() {
    $scope.loadBookmarksSpin = true
    apiBookmarksService.findBookmarks(function(/*[Bookmark]*/ bookmarks) {

      $scope.managedBookmarks = bookmarks

      $timeout(function() { $scope.loadBookmarksSpin = false }, 1000)

    })
  }

  $scope.showAddBookmarkFragment = function() {
    $scope.newBookmark = new NewBookmark()
    addBookmarkDialog.show()
  }

  $scope.submitNewBookmark = function() {
    apiBookmarksService.addBookmark($scope.newBookmark.toApiBookmarkEntity(), function() {
      notifyService.notify('New bookmark added', 'success')
      $scope.loadBookmarks()
    });
  }

  var findBookmarkByIdToEdit = function(/*str*/ id) {
    $scope.editedBookmark = null

    var bookmarks = $.grep($scope.managedBookmarks, function(bookmark) { return bookmark.id == id })
    if (bookmarks.length > 0) $scope.editedBookmark = new EditedBookmark(bookmarks[0])

    return $scope.editedBookmark
  }

  $scope.showEditBookmarkFragment = function(/*str*/ bookmarkId) {
    findBookmarkByIdToEdit(bookmarkId)
  }

  $scope.showManageBookmarksFragment = function() {
    delete $scope.editedBookmark
  }

  $scope.refreshEditedBookmark = function() {
    $scope.refreshEditedBookmarkSpin = true

    $scope.editedBookmark.refresh(function() {

      findBookmarkByIdToEdit($scope.editedBookmark.id)
      $timeout(function() { $scope.refreshEditedBookmarkSpin = false }, 1000)

    })
  }

  $scope.submitEditedBookmark = function() {
    apiBookmarksService.patchBookmark($scope.editedBookmark.id, $scope.editedBookmark.toApiBookmarkEntity(), function() {
      notifyService.notify('Bookmark updated', 'success')
      $scope.refreshEditedBookmark()
    });
  }

  $scope.deleteEditedBookmark = function() {
    apiBookmarksService.deleteBookmark($scope.editedBookmark.id, function() {
      notifyService.notify('Bookmark deleted', 'success')
      delete $scope.editedBookmark
      $scope.loadBookmarks()
    });
  }

  var addBookmarkDialog = modalDialogService.registerDialog('#manage-bookmarks-add-dialog')

  $scope.loadBookmarks()
})
