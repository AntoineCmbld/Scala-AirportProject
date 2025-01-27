package airportproject.components.Objects

import airportproject.components.CsvParser
import cats.syntax.all._
import cats.instances.list._


// -------------------- COUNTRY TYPE -------------------- //

case class Country(
  id: Int,
  code: String,
  name: String,
  continent: String,
  wikipediaLink: Option[String],
  keywords: Option[String]
)

object Country {
  def fromCsv(line: String): Either[Throwable, Country] = {
    val parts = CsvParser.splitCsvLine(line)
    Either.catchNonFatal(parts(0).toInt).map { id =>
      Country(
        id = id,
        code = parts(1),
        name = parts(2),
        continent = parts(3),
        wikipediaLink = parts.lift(4).filter(_.nonEmpty),
        keywords = parts.lift(5).filter(_.nonEmpty)
      )
    }
  }
}