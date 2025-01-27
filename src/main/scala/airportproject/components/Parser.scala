package airportproject.components

import scala.io.Source
import cats.syntax.all._
import cats.instances.list._
import airportproject.components.Objects._
import cats.effect.{IO, IOApp}



// -------------------- CSV PARSER -------------------- //

object CsvParser {
  def parseFile[T](filePath: String, fromCsv: String => Either[Throwable, T]): Either[Throwable, List[T]] = {
    val sourceEither = Either.catchNonFatal(Source.fromFile(filePath))
    
    sourceEither.flatMap { source =>
      val result = Either.catchNonFatal {
        val lines = source.getLines().drop(1) // Skip header
        lines.map(fromCsv).toList.sequence
      }
      
      source.close()
      result.flatten
    }
  }

  def splitCsvLine(line: String): Array[String] = {
    line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")
      .map(_.trim.stripPrefix("\"").stripSuffix("\""))
  }

  def readCountries: IO[List[Country]] = {
    val path = "src/main/ressource/countries.csv"
    IO.fromEither(CsvParser.parseFile(path, Country.fromCsv))
      .handleErrorWith(e => 
        IO.raiseError(new Exception(s"Failed to parse countries CSV: ${e.getMessage}"))
      )
  }

  def readAirports: IO[List[Airport]] = {
    val path = "src/main/ressource/airports.csv"
    IO.fromEither(CsvParser.parseFile(path, Airport.fromCsv))
      .handleErrorWith(e => 
        IO.raiseError(new Exception(s"Failed to parse airports CSV: ${e.getMessage}"))
      )
  }

  def readRunways: IO[List[Runway]] = {
    val path = "src/main/ressource/runways.csv"
    IO.fromEither(CsvParser.parseFile(path, Runway.fromCsv))
      .handleErrorWith(e => 
        IO.raiseError(new Exception(s"Failed to parse runways CSV: ${e.getMessage}"))
      )
  }
}