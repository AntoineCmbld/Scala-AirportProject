package airportproject.components

import cats.effect.{IO, ExitCode}
import cats.syntax.all._
import AnsiColors._

object GUI {
  def clearConsole: IO[Unit] = IO.println("\u001b[2J")

  def printMessage(message: String): IO[Unit] = IO.println(message)

  def start: IO[ExitCode] = 
    displayWelcome *> programLoop

  def displayInit: IO[Unit] = IO.println(
    s"""
    |${BOLD}${CYAN}=================================${RESET}
    |${BOLD}${GREEN}     Database Initializing  ${RESET}
    |${BOLD}${CYAN}=================================${RESET}
    """.stripMargin
  )

  def displayWelcome: IO[Unit] = IO.println(
    s"""
    |${BOLD}${CYAN}=================================${RESET}
    |${BOLD}${GREEN}   Airport Information System   ${RESET}
    |${BOLD}${CYAN}=================================${RESET}
    """.stripMargin
  )

  private def programLoop: IO[ExitCode] = 
    mainMenu.flatMap(handleMainChoice).flatMap(result => 
      if (result == ExitCode.Success) programLoop 
      else IO.pure(result)
    )

  private def mainMenu: IO[String] = 
    IO.println(s"\n${BOLD}Main Menu:${RESET}") *>
    IO.println(s"${GREEN}1.${RESET} Query") *>
    IO.println(s"${GREEN}2.${RESET} Reports") *>
    IO.println(s"${RED}3.${RESET} Exit") *>
    IO.println(s"Enter your choice (${GREEN}1-3${RESET}): ") *>
    IO.readLine

  private def handleMainChoice(choice: String): IO[ExitCode] = choice match {
    case "1" => handleQuery.as(ExitCode.Success)
    case "2" => handleReports.as(ExitCode.Success)
    case "3" => IO.println(s"${BLUE}Thank you for using our project. Goodbye!${RESET}")
                 .as(ExitCode(1))
    case _ => IO.println(s"${RED}Invalid choice, please try again${RESET}")
               .as(ExitCode.Success)
  }

  private def handleQuery: IO[Unit] = 
    IO.println(s"\n${BOLD}=== Query Mode ===${RESET}") *>
    IO.println(s"${BOLD}Enter country name or code: ${RESET}") *>
    IO.readLine.flatMap(input =>
      IO.println(s"\n${BOLD}Searching for: ${CYAN}$input${RESET}") *>
      DatabaseService.getCountryByCodeOrName(input).flatMap(countries => {
        val matchingCountry = countries.headOption
        matchingCountry.fold(
          IO.println(s"${RED}No matching country found for: $input${RESET}")
        )(country =>
          DatabaseService.getAirportsByCountry(country.code).flatMap(airports =>
            if (airports.isEmpty) 
              IO.println(s"${YELLOW}No airports found for ${country.name}${RESET}")
            else 
              IO.println(s"\n${BOLD}Found ${GREEN}${airports.length}${RESET}${BOLD} airports in ${country.name}:${RESET}") *>
              airports.traverse_(airport =>
                IO.println(s"\n${GREEN}${airport.name}${RESET}") *>
                IO.println(s"  Location: ${airport.municipality.getOrElse("Unknown")}") *>
                DatabaseService.getRunwaysByAirport(airport.id).flatMap(runways =>
                  if (runways.isEmpty)
                    IO.println(s"  ${GRAY}No runway information available${RESET}")
                  else
                    IO.println(s"  ${BOLD}Runways (${runways.length}):${RESET}") *>
                    runways.traverse_(runway =>
                      IO.println(
                        s"    - Length: ${runway.lengthFt.map(_.toString).getOrElse("N/A")}ft, " +
                        s"Surface: ${runway.surface.getOrElse("Unknown")}, " +
                        s"ID: ${runway.leIdent.getOrElse("N/A")}"
                      )
                    )
                )
              )
          )
        )
      })
    )

  private def handleReports: IO[Unit] = 
    displayReportsMenu.flatMap(handleReportChoice)

  private def displayReportsMenu: IO[String] = 
    IO.println(s"\n${BOLD}=== Reports Menu ===${RESET}") *>
    IO.println(s"${GREEN}1.${RESET} Countries with highest/lowest number of airports") *>
    IO.println(s"${GREEN}2.${RESET} Types of runways per country") *>
    IO.println(s"${GREEN}3.${RESET} Most common runway identifiers") *>
    IO.println(s"${RED}4.${RESET} Back to main menu") *>
    IO.println(s"Select report (${GREEN}1-4${RESET}): ") *>
    IO.readLine

  private def handleReportChoice(choice: String): IO[Unit] = choice match {
    case "1" => showAirportCounts
    case "2" => showRunwayTypes
    case "3" => showRunwayLatitudes
    case "4" => IO.unit
    case _ => IO.println(s"${RED}Invalid choice, please try again${RESET}")
  }

  private def showAirportCounts: IO[Unit] = 
    IO.println(s"\n${BOLD}${GREEN}10 Countries with Highest Number of Airports:${RESET}") *>
    DatabaseService.getTopAirportCounts.flatMap(topCountries =>
      topCountries.zipWithIndex.traverse_{ case ((name, count), idx) =>
        IO.println(f"${idx + 1}%-2d. ${CYAN}$name: ${GREEN}$count airports${RESET}")
      } *>
      IO.println("") *>
      IO.println(s"\n${BOLD}${YELLOW}Countries with Lowest Number of Airports:${RESET}") *>
      DatabaseService.getBottomAirportCounts.flatMap(bottomCountries =>
        bottomCountries.zipWithIndex.traverse_{ case ((name, count), idx) =>
          IO.println(f"${idx + 1}%-2d. ${CYAN}$name: ${YELLOW}$count airports${RESET}")
        } *>
        IO.println("\nPress Enter to continue...") *>
        IO.readLine.void
      )
    )

  private def showRunwayTypes: IO[Unit] = 
    IO.println(s"\n${BOLD}${GREEN}Runway Types by Country:${RESET}") *>
    DatabaseService.getRunwayTypesPerCountry.map(_.groupBy(_._1)).flatMap(groupedTypes =>
      groupedTypes.toList.sortBy(_._1).traverse_{ case (country, surfaces) =>
        IO.println(s"\n${BOLD}${CYAN}$country${RESET}") *>
        surfaces.sortBy(-_._3).traverse_{ case (_, surface, count) =>
          IO.println(f"  • ${surface}%-20s: $count runways")
        }
      } *>
      IO.println("\nPress Enter to continue...") *>
      IO.readLine.void
    )

  private def showRunwayLatitudes: IO[Unit] = 
    IO.println(s"\n${BOLD}${GREEN}Top 10 Most Common Runway Latitudes:${RESET}") *>
    IO.println(s"(Based on le_ident column)\n") *>
    DatabaseService.getCommonRunwayLatitudes.flatMap(latitudes =>
      latitudes.zipWithIndex.traverse_{ case ((ident, count), idx) =>
        IO.println(f"${idx + 1}%-2d. ${CYAN}Latitude $ident: ${GREEN}$count occurrences${RESET}")
      } *>
      IO.println("\nPress Enter to continue...") *>
      IO.readLine.void
    )
}