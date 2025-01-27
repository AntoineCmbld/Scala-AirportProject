package airportproject.components.Objects

import airportproject.components.CsvParser
import cats.syntax.all._
import cats.instances.list._


// -------------------- AIRPORT TYPE -------------------- //

case class Airport(
  id: Int,
  ident: String,
  airportType: String,
  name: String,
  latitude: Option[Double],
  longitude: Option[Double],
  elevation: Option[Int],
  continent: String,
  isoCountry: String,
  isoRegion: String,
  municipality: Option[String],
  scheduledService: String,
  gpsCode: Option[String],
  iataCode: Option[String],
  localCode: Option[String],
  homeLink: Option[String],
  wikipediaLink: Option[String],
  keywords: Option[String]
)

object Airport {
  def fromCsv(line: String): Either[Throwable, Airport] = {
    val parts = CsvParser.splitCsvLine(line)
    val idEither = Either.catchNonFatal(parts(0).toInt)
    val latitudeEither = parts.lift(4).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toDouble))
    val longitudeEither = parts.lift(5).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toDouble))
    val elevationEither = parts.lift(6).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toInt))

    (idEither, latitudeEither, longitudeEither, elevationEither).mapN { (id, latitude, longitude, elevation) =>
      Airport(
      id = id,
      ident = parts(1),
      airportType = parts(2),
      name = parts(3),
      latitude = latitude,
      longitude = longitude,
      elevation = elevation,
      continent = parts(7),
      isoCountry = parts(8),
      isoRegion = parts(9),
      municipality = parts.lift(10).filter(_.nonEmpty),
      scheduledService = parts(11),
      gpsCode = parts.lift(12).filter(_.nonEmpty),
      iataCode = parts.lift(13).filter(_.nonEmpty),
      localCode = parts.lift(14).filter(_.nonEmpty),
      homeLink = parts.lift(15).filter(_.nonEmpty),
      wikipediaLink = parts.lift(16).filter(_.nonEmpty),
      keywords = parts.lift(17).filter(_.nonEmpty)
      )
    }
  }
}