# Microservices with Scala

This is the example application of microservices and UI.
For the project's overview [check out this article](http://contented-cows.blogspot.com/2017/02/microservices-with-scala.html).

Feedback email cftp@coldcore.com

## Technologies ##

* Backend: Scala, Akka HTTP, Akka Actors, MongoDB, SBT, Casbah, Spay JSON
* Frontend: AngularJS, Bootstrap, jQuery, Node JS

## Running the backend ##

* Install and run MongoDB
* Install [SBT](http://www.scala-sbt.org)
* From the project's root directory do `sbt run`

## Running the frontend ##

* Install [Node](http://www.scala-sbt.org)
* From the project's weblet directory do `npm install express request` and then `node weblet.js`
* Open `http://localhost:8080/cp` to access the UI
* Populate the sample database data and then use "admin" as both username and password

## Testing ##

* Requires MongoDB up and running
* From the project's root directory do `sbt it:test`

## Sample database data ##

* Connect to your MongoDB
* `use favorites-test`
* `db['ms-auth-users'].insert({ username: 'admin', password: 'admin' })`
* `db['ms-profiles'].insert({ username: 'admin', email: 'admin@example.org' })`

## License ##

This code is open source software licensed under the [GNU Lesser General Public License v3](http://www.gnu.org/licenses/lgpl-3.0.en.html).
