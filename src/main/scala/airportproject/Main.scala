package airportproject

import cats.effect.{ExitCode, IO, IOApp}
import airportproject.components.DatabaseService
import airportproject.components.{CsvParser}
import cats.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration._
import airportproject.api.AirportApi
import org.typelevel.log4cats.slf4j.loggerFactoryforSync
import airportproject.components.Objects.{Country, Airport, Runway}
import airportproject.components.CsvParser._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
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
    airportproject.components.GUI.start.as(ExitCode.Success)
    .handleErrorWith { error => 
      components.GUI.printMessage(s"An error occurred: ${error.getMessage}")
      .as(ExitCode.Error)
    }
  }

  private def loadData: IO[(List[Country], List[Airport], List[Runway])] = {
    val countryData = readCountries
    val airportData = readAirports  
    val runwayData = readRunways

    (countryData, airportData, runwayData).mapN { (countries, airports, runways) =>
      (countries, airports, runways)
    }
  }
}