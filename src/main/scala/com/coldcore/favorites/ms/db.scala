package com.coldcore.favorites
package ms.db

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.util.JSON
import org.bson.types.ObjectId
import spray.json._

trait MongoQueries {

  def update(finder: DBObject, collection: MongoCollection, key: String, value: Option[Any]): Unit =
    value.foreach { v => update(finder, collection, MongoDBObject(key -> v)) }

  def update(finder: DBObject, collection: MongoCollection, key: String, value: Any): Unit =
    update(finder, collection, MongoDBObject(key -> value))

  def update(finder: DBObject, collection: MongoCollection, elements: DBObject): Unit =
    collection.findAndModify(finder, MongoDBObject("$set" -> elements))

  def asDBObject[A](a: A)(implicit writer: JsonWriter[A]): DBObject =
    JSON.parse(a.toJson.toString).asInstanceOf[DBObject]

  def asDBObject(a: JsObject): DBObject =
    JSON.parse(a.toString).asInstanceOf[DBObject]

  implicit class DBOpbectExt(obj: DBObject) {
    def idString: String = obj.getAs[ObjectId]("_id").get.toString
  }

  def finderById(id: String): DBObject =
    "_id" $eq new ObjectId(id)

}
