package airportproject

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.server.blaze.BlazeServerBuilder
import airportproject.api.AirportApi
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.slf4j.loggerFactoryforSync
import airportproject.components.DatabaseService
import airportproject.components.CsvParser._
import cats.implicits._
import airportproject.components.Objects.{Country, Airport, Runway}

object API extends IOApp.Simple {

  private def loadData: IO[(List[Country], List[Airport], List[Runway])] = {
    val countryData = readCountries
    val airportData = readAirports  
    val runwayData = readRunways

    (countryData, airportData, runwayData).mapN { (countries, airports, runways) =>
      (countries, airports, runways)
    }
  }

  def run: IO[Unit] = {

    components.GUI.clearConsole >>
    components.GUI.displayInit >>
    DatabaseService.createTableIfNotExists >>
    components.GUI.printMessage("Loading data...") >>
    loadData.flatMap { data =>
      components.GUI.printMessage("Data loaded successfully") >>
      {
      val (countries, airports, runways) = data
      components.GUI.printMessage("Populating database...") >>
      DatabaseService.populateDatabase(countries, airports, runways) >>
      components.GUI.printMessage("Database populated successfully\n ")
      }
    } >>
    BlazeServerBuilder[IO]
      .bindHttp(8082, "0.0.0.0")
        .withHttpApp(AirportApi.routes.orNotFound)
        .serve
        .compile
        .drain
        
    .handleErrorWith { error => 
      components.GUI.printMessage(s"An error occurred: ${error.getMessage}")
      .as(ExitCode.Error)
    }
  }
}