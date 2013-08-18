package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.{Writes, Json, JsValue}


case class MovieS (id: Pk[Long], title: String, year: Int)

object MovieS {

  val movieParser = {
    get[Pk[Long]]("pelicula_id") ~
    get[String]("nombre_en") ~
    get[Int]("anio") map {
      case id~title~year => MovieS(id, title, year)
    }
  }

  implicit def valueToJson[E: Writes] = new Writes[Pk[E]]{
    def writes(value: Pk[E]): JsValue = Json.toJson(value.get)
  }

  implicit val movieWrites = new Writes[MovieS]{
    def writes(movie: MovieS): JsValue = {
      Json.obj(
        "id" -> movie.id,
        "title" -> movie.title,
        "year" -> movie.year
      )
    }
  }

  // -- Queries

//  val computers = SQL(
//    """
//          select * from computer
//          left join company on computer.company_id = company.id
//          where computer.name like {filter}
//          order by {orderBy} nulls last
//          limit {pageSize} offset {offset}
//    """
//  ).on(
//    'pageSize -> pageSize,
//    'offset -> offest,
//    'filter -> filter,
//    'orderBy -> orderBy
//  ).as(Computer.withCompany *)

  def list(offset: Int, limit:Int, orderBy:Int):List[MovieS] = {
    val movies = DB.withConnection { implicit connection =>
      SQL(
        """
          SELECT p.pelicula_id, p.guion, p.director, p.nombre_en, p.anio, p.pais
          from pelicula p WHERE p.anio = 2012
          order by p.nombre_en asc
          limit {limit} offset {offset}
        """
      ).on(
        "limit" -> limit,
        "offset" -> offset
      ).as(movieParser *)
    }

    for(movie <- movies){
      println(movie.id)
    }
    movies
  }

  /*esto tal vez no va aqui porque regresa usuario, pelicula, puntuacion*/
//  def users(userId: Long) = {
//    val simUsers = DB.withConnection { implicit connection =>
//      SQL(
//        """
//          select p.* from puntuacion p, (
//          select distinct a.usuario_id usuario_id from puntuacion a, puntuacion b
//          where a.pelicula_id = b.pelicula_id and b.usuario_id = {userId} AND a.usuario_id <> {userId}) q
//          where p.usuario_id = q.usuario_id order by p.usuario_id asc, p.puntuacion desc;
//        """
//      ).on(
//        "userId" -> userId
//      ).as(movieParser *)
//    }
//
//    for(Movie ){
//      print()
//    }
//  }

}
