package airportproject.api

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import airportproject.components.DatabaseService
import airportproject.components.Objects.{Airport, Runway}
import cats.implicits._
import airportproject.components.Objects.Country
import airportproject.components.DatabaseService.getStats


object AirportApi {
  // Response models
  case class CountryAirportCount(country: String, airportCount: Long)
  case class RunwayType(country: String, surface: String, count: Long)
  case class RunwayLatitude(identifier: String, count: Long)
  case class Stats(countryCode: String, airportCount: Int, runwayCount: Int, avgRunwaysPerAirport: Double)
  case class AirportDetails(name: String, municipality: Option[String], runways: List[RunwayInfo])
  case class RunwayInfo(length: Option[Int], surface: Option[String], identifier: Option[String])
  case class CountryResponse(countryName: String, airports: List[AirportDetails])

  private def mapRunwayToInfo(runway: Runway): RunwayInfo = 
    RunwayInfo(
      length = runway.lengthFt,
      surface = runway.surface,
      identifier = runway.leIdent
    )

  private def mapAirportToDetails(airport: Airport, runways: List[Runway]): AirportDetails =
    AirportDetails(
      name = airport.name,
      municipality = airport.municipality,
      runways = runways.map(mapRunwayToInfo)
    )

  private def processCountry(country: Country): IO[CountryResponse] =
    DatabaseService.getAirportsByCountry(country.code).flatMap { airports =>
      airports.traverse { airport =>
        DatabaseService.getRunwaysByAirport(airport.id).map(runways =>
          mapAirportToDetails(airport, runways)
        )
      }.map(airportDetails => CountryResponse(country.name, airportDetails))
    }

  def routes: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "top-airports" =>
        DatabaseService.getTopAirportCounts.flatMap { counts =>
          val response = counts.map { case (country, count) => 
            CountryAirportCount(country, count)
          }
          Ok(response.asJson)
        }

      case GET -> Root / "bottom-airports" =>
        DatabaseService.getBottomAirportCounts.flatMap { counts =>
          val response = counts.map { case (country, count) => 
            CountryAirportCount(country, count)
          }
          Ok(response.asJson)
        }

      case GET -> Root / "runway-types" =>
        DatabaseService.getRunwayTypesPerCountry.flatMap { types =>
          val response = types.map { case (country, surface, count) => 
            RunwayType(country, surface, count)
          }
          Ok(response.asJson)
        }

      case GET -> Root / "common-latitudes" =>
        DatabaseService.getCommonRunwayLatitudes.flatMap { latitudes =>
          val response = latitudes.map { case (ident, count) => 
            RunwayLatitude(ident, count)
          }
          Ok(response.asJson)
        }

      case GET -> Root / "countries" =>
        DatabaseService.getCountries.flatMap(countries => Ok(countries.asJson))

      case GET -> Root / "query" / countryCode =>
      DatabaseService.getCountryByCodeOrName(countryCode)
        .flatMap(_.traverse(processCountry))
        .flatMap(responses => Ok(responses.asJson))
        .handleErrorWith(error => InternalServerError(s"An error occurred: ${error.getMessage}"))

      case GET -> Root / "airports" / countryCode =>
        DatabaseService.getAirportsByCountry(countryCode)
          .flatMap(airports => Ok(airports.asJson))

      case GET -> Root / "runways" / IntVar(airportId) =>
        DatabaseService.getRunwaysByAirport(airportId)
          .flatMap(runways => Ok(runways.asJson))

      case GET -> Root / "stats" / countryCode =>
        getStats(countryCode).flatMap(stats => Ok(stats.asJson))
    }
  }
}