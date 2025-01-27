package airportproject.components.Objects

import airportproject.components.CsvParser
import cats.syntax.all._
import cats.instances.list._


// -------------------- RUNWAY TYPE -------------------- //

case class Runway(
  id: Int,
  airportRef: Int,
  airportIdent: String,
  lengthFt: Option[Int],
  widthFt: Option[Int],
  surface: Option[String],
  lighted: Boolean,
  closed: Boolean,
  leIdent: Option[String],
  leLatitude: Option[Double],
  leLongitude: Option[Double],
  leElevationFt: Option[Int],
  leHeadingDegT: Option[Double],
  leDisplacedThresholdFt: Option[Int],
  heIdent: Option[String],
  heLatitude: Option[Double],
  heLongitude: Option[Double],
  heElevationFt: Option[Int],
  heHeadingDegT: Option[Double],
  heDisplacedThresholdFt: Option[Int]
)

object Runway {
  def fromCsv(line: String): Either[Throwable, Runway] = {
    val parts = CsvParser.splitCsvLine(line)
    val idEither = Either.catchNonFatal(parts(0).toInt)
    val airportRefEither = Either.catchNonFatal(parts(1).toInt)
    val lengthFtEither = parts.lift(3).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toInt))
    val widthFtEither = parts.lift(4).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toInt))
    val leLatitudeEither = parts.lift(9).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toDouble))
    val leLongitudeEither = parts.lift(10).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toDouble))
    val leElevationFtEither = parts.lift(11).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toInt))
    val leHeadingDegTEither = parts.lift(12).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toDouble))
    val leDisplacedThresholdFtEither = parts.lift(13).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toInt))
    val heLatitudeEither = parts.lift(15).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toDouble))
    val heLongitudeEither = parts.lift(16).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toDouble))
    val heElevationFtEither = parts.lift(17).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toInt))
    val heHeadingDegTEither = parts.lift(18).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toDouble))
    val heDisplacedThresholdFtEither = parts.lift(19).filter(_.nonEmpty).traverse(s => Either.catchNonFatal(s.toInt))

    (idEither, airportRefEither, lengthFtEither, widthFtEither, leLatitudeEither, leLongitudeEither, leElevationFtEither, leHeadingDegTEither, leDisplacedThresholdFtEither, heLatitudeEither, heLongitudeEither, heElevationFtEither, heHeadingDegTEither, heDisplacedThresholdFtEither).mapN {
      (id, airportRef, lengthFt, widthFt, leLatitude, leLongitude, leElevationFt, leHeadingDegT, leDisplacedThresholdFt, heLatitude, heLongitude, heElevationFt, heHeadingDegT, heDisplacedThresholdFt) =>
      Runway(
        id = id,
        airportRef = airportRef,
        airportIdent = parts(2),
        lengthFt = lengthFt,
        widthFt = widthFt,
        surface = parts.lift(5).filter(_.nonEmpty),
        lighted = parts.lift(6).exists(_ == "1"),
        closed = parts.lift(7).exists(_ == "1"),
        leIdent = parts.lift(8).filter(_.nonEmpty),
        leLatitude = leLatitude,
        leLongitude = leLongitude,
        leElevationFt = leElevationFt,
        leHeadingDegT = leHeadingDegT,
        leDisplacedThresholdFt = leDisplacedThresholdFt,
        heIdent = parts.lift(14).filter(_.nonEmpty),
        heLatitude = heLatitude,
        heLongitude = heLongitude,
        heElevationFt = heElevationFt,
        heHeadingDegT = heHeadingDegT,
        heDisplacedThresholdFt = heDisplacedThresholdFt
      )
    }
  }
}