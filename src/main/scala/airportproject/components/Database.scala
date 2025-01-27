package airportproject.components

import cats.effect.{IO, Resource}
import cats.syntax.all._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import org.typelevel.otel4s.trace.Tracer.Implicits.noop
import cats.instances.partialOrder
import airportproject.components.Objects.{Country, Airport, Runway}
import airportproject.api.AirportApi.Stats


object DatabaseService {

  // -------------------- DB SETUP -------------------- //

  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      port = 5432,
      user = "postgres_user",
      database = "postgres_db",
      password = Some("postgres_password")
    )


  // -------------------- TABLE CREATION -------------------- //

  def createTableIfNotExists: IO[Unit] =
    session.use { s =>

      // AIRPORTS TABLE
      val createAirportsTable: Command[Void] =
        sql"""
          CREATE TABLE IF NOT EXISTS airports (
            id INT PRIMARY KEY,
            ident VARCHAR,
            type VARCHAR,
            name VARCHAR,
            latitude_deg DOUBLE PRECISION,
            longitude_deg DOUBLE PRECISION,
            elevation_ft INT,
            continent VARCHAR,
            iso_country VARCHAR,
            iso_region VARCHAR,
            municipality VARCHAR,
            scheduled_service VARCHAR,
            gps_code VARCHAR,
            iata_code VARCHAR,
            local_code VARCHAR,
            home_link VARCHAR,
            wikipedia_link VARCHAR,
            keywords VARCHAR
          )
        """.command
      
      // COUNTRIES TABLE
      val createCountriesTable: Command[Void] =
        sql"""
          CREATE TABLE IF NOT EXISTS countries (
            id INT PRIMARY KEY,
            code VARCHAR,
            name VARCHAR,
            continent VARCHAR,
            wikipedia_link VARCHAR,
            keywords VARCHAR
          )
        """.command
      
      // RUNWAYS TABLE
      val createRunwaysTable: Command[Void] =
        sql"""
          CREATE TABLE IF NOT EXISTS runways (
            id INT PRIMARY KEY,
            airport_ref INT,
            airport_ident VARCHAR,
            length_ft INT,
            width_ft INT,
            surface VARCHAR,
            lighted BOOLEAN,
            closed BOOLEAN,
            le_ident VARCHAR,
            le_latitude_deg DOUBLE PRECISION,
            le_longitude_deg DOUBLE PRECISION,
            le_elevation_ft INT,
            le_heading_degT DOUBLE PRECISION,
            le_displaced_threshold_ft INT,
            he_ident VARCHAR,
            he_latitude_deg DOUBLE PRECISION,
            he_longitude_deg DOUBLE PRECISION,
            he_elevation_ft INT,
            he_heading_degT DOUBLE PRECISION,
            he_displaced_threshold_ft INT
          )
        """.command

      s.execute(createAirportsTable).void *>
      s.execute(createCountriesTable).void *>
      s.execute(createRunwaysTable).void
    }



  // -------------------- INSERT METHODS -------------------- //

  // Insert a country into the database
  private def insertCountry(country: Country, s: Session[IO]): IO[Unit] = {
    val query: Command[Country] = sql"""
      INSERT INTO countries (
        id, code, name, continent, wikipedia_link, keywords
      ) VALUES (
        $int4, $varchar, $varchar, $varchar, ${varchar.opt}, ${varchar.opt}
      ) ON CONFLICT (id) DO NOTHING
    """.command.contramap { c =>
      (c.id, c.code, c.name, c.continent, c.wikipediaLink, c.keywords)
    }

    Resource.eval(s.prepare(query)).use(pc => pc.execute(country)).void
  }

  // Insert an airport into the database
  private def insertAirport(airport: Airport, s: Session[IO]): IO[Unit] = {
    val query: Command[Airport] = sql"""
      INSERT INTO airports (
        id, ident, type, name, latitude_deg, longitude_deg, elevation_ft,
        continent, iso_country, iso_region, municipality, scheduled_service,
        gps_code, iata_code, local_code, home_link, wikipedia_link, keywords
      ) VALUES (
        $int4, $varchar, $varchar, $varchar, ${float8.opt}, ${float8.opt},
        ${int4.opt}, $varchar, $varchar, $varchar, ${varchar.opt},
        $varchar, ${varchar.opt}, ${varchar.opt}, ${varchar.opt},
        ${varchar.opt}, ${varchar.opt}, ${varchar.opt}
      ) ON CONFLICT (id) DO NOTHING
    """.command.contramap { a =>
      (a.id, a.ident, a.airportType, a.name, a.latitude, a.longitude,
       a.elevation, a.continent, a.isoCountry, a.isoRegion, a.municipality,
       a.scheduledService, a.gpsCode, a.iataCode, a.localCode,
       a.homeLink, a.wikipediaLink, a.keywords)
    }

    Resource.eval(s.prepare(query)).use(pc => pc.execute(airport)).void
  }

  // Insert a runway into the database
  private def insertRunway(runway: Runway, s: Session[IO]): IO[Unit] = {
    val query: Command[Runway] = sql"""
      INSERT INTO runways (
        id, airport_ref, airport_ident, length_ft, width_ft, surface, lighted,
        closed, le_ident, le_latitude_deg, le_longitude_deg, le_elevation_ft,
        le_heading_degT, le_displaced_threshold_ft, he_ident, he_latitude_deg,
        he_longitude_deg, he_elevation_ft, he_heading_degT, he_displaced_threshold_ft
      ) VALUES (
        $int4, $int4, $varchar, ${int4.opt}, ${int4.opt}, ${varchar.opt}, $bool,
        $bool, ${varchar.opt}, ${float8.opt}, ${float8.opt}, ${int4.opt}, ${float8.opt},
        ${int4.opt}, ${varchar.opt}, ${float8.opt}, ${float8.opt}, ${int4.opt},
        ${float8.opt}, ${int4.opt}
      ) ON CONFLICT (id) DO NOTHING
    """.command.contramap { r =>
      (r.id, r.airportRef, r.airportIdent, r.lengthFt, r.widthFt, r.surface,
      r.lighted, r.closed, r.leIdent, r.leLatitude, r.leLongitude, r.leElevationFt,
      r.leHeadingDegT, r.leDisplacedThresholdFt, r.heIdent, r.heLatitude, r.heLongitude,
      r.heElevationFt, r.heHeadingDegT, r.heDisplacedThresholdFt)
    }

    Resource.eval(s.prepare(query)).use(pc => pc.execute(runway)).void
  }



  // -------------------- INTEGRITY CHECK -------------------- //

  def lengthCountry: IO[Long] = {
    session.use { s =>
      val query: Query[Void, Long] = sql"""
        SELECT COUNT(*)::bigint FROM countries
      """.query(int8)

      s.prepare(query).flatMap(_.unique(Void))
    }
  }

  def lengthAirport: IO[Long] = {
    session.use { s =>
      val query: Query[Void, Long] = sql"""
        SELECT COUNT(*)::bigint FROM airports
      """.query(int8)

      s.prepare(query).flatMap(_.unique(Void))
    }
  }

  def lengthRunway: IO[Long] = {
    session.use { s =>
      val query: Query[Void, Long] = sql"""
        SELECT COUNT(*)::bigint FROM runways
      """.query(int8)

      s.prepare(query).flatMap(_.unique(Void))
    }
  }



  // -------------------- DATA RETRIEVAL FROM CSV TO DB -------------------- //

  def populateDatabase(
      countries: List[Country],
      airports: List[Airport],
      runways: List[Runway]
  ): IO[Unit] = {
    session.use { s =>
      GUI.printMessage("Populating database...").flatMap(_ =>
        lengthCountry.flatMap { countryLen =>
          if (countryLen == 247L) {
            GUI.printMessage("Countries data already populated, skipping...")
          } else {
            GUI.printMessage(s"Inserting ${countries.size} countries...").flatMap(_ =>
              countries.traverse_(insertCountry(_, s)).flatMap(_ =>
                GUI.printMessage("Countries inserted.")
              )
            )
          }
        }.flatMap(_ =>
          lengthAirport.flatMap { airportLen =>
            if (airportLen == 46505L) {
              GUI.printMessage("Airports data already populated, skipping...")
            } else {
              GUI.printMessage(s"Inserting ${airports.size} airports...").flatMap(_ =>
                airports.traverse_(insertAirport(_, s)).flatMap(_ =>
                  GUI.printMessage("Airports inserted.")
                )
              )
            }
          }.flatMap(_ =>
            lengthRunway.flatMap { runwayLen =>
              if (runwayLen == 39536L) {
                GUI.printMessage("Runways data already populated, skipping...")
              } else {
                GUI.printMessage(s"Inserting ${runways.size} runways...").flatMap(_ =>
                  runways.traverse_(insertRunway(_, s)).flatMap(_ =>
                    GUI.printMessage("Runways inserted.")
                  )
                )
              }
            }
          )
        )
      )
    }
  }




  // -------------------- DATA RETRIEVAL FROM DB -------------------- //

  def getCountries: IO[List[Country]] = session.use { s =>
    val query: Query[Void, Country] = sql"""
      SELECT id, code, name, continent, wikipedia_link, keywords 
      FROM countries
    """.query(int4 ~ varchar ~ varchar ~ varchar ~ varchar.opt ~ varchar.opt)
      .map { 
        case ((((((id, code), name), continent), wiki), keys)) =>
          Country(id, code, name, continent, wiki, keys)
      }
    s.prepare(query).flatMap(_.stream(Void, 64).compile.toList)
  }

  def getCountryByCodeOrName(partialNameOrCode: String): IO[List[Country]] = session.use { s =>
    val isCodeSearch = partialNameOrCode.trim.length == 2
    val trimmedInput = partialNameOrCode.trim

    // Query for searching by code
    val codeQuery: Query[String, Country] = sql"""
      SELECT id, code, name, continent, wikipedia_link, keywords
      FROM countries
      WHERE code ILIKE $varchar
      LIMIT 1
    """.query(int4 ~ varchar ~ varchar ~ varchar ~ varchar.opt ~ varchar.opt)
      .map {
        case ((((((id, code), name), continent), wiki), keys)) =>
          Country(id, code, name, continent, wiki, keys)
      }

    // Query for searching by name
    val nameQuery: Query[String, Country] = sql"""
      SELECT id, code, name, continent, wikipedia_link, keywords
      FROM countries
      WHERE name ILIKE '%' || $varchar || '%'
    """.query(int4 ~ varchar ~ varchar ~ varchar ~ varchar.opt ~ varchar.opt)
      .map {
        case ((((((id, code), name), continent), wiki), keys)) =>
          Country(id, code, name, continent, wiki, keys)
      }

    // Execute the appropriate query based on input length
    val query = if (isCodeSearch) codeQuery else nameQuery
    s.prepare(query).flatMap(_.stream(trimmedInput, 64).compile.toList)
  }


  def getAirportsByCountry(partialNameOrCode: String): IO[List[Airport]] = session.use { s =>
      val query: Query[(String, String), Airport] = sql"""
        SELECT a.id, a.ident, a.type, a.name, a.latitude_deg, a.longitude_deg, a.elevation_ft,
              a.continent, a.iso_country, a.iso_region, a.municipality, a.scheduled_service,
              a.gps_code, a.iata_code, a.local_code, a.home_link, a.wikipedia_link, a.keywords
        FROM airports a
        JOIN countries c ON a.iso_country = c.code
        WHERE c.name ILIKE '%' || $varchar || '%' OR c.code ILIKE $varchar
      """.query(
        int4 ~ varchar ~ varchar ~ varchar ~ float8.opt ~ float8.opt ~ int4.opt ~
        varchar ~ varchar ~ varchar ~ varchar.opt ~ varchar ~
        varchar.opt ~ varchar.opt ~ varchar.opt ~ varchar.opt ~ varchar.opt ~ varchar.opt
      ).map {
        case ((((((((((((((((((id, ident), typ), name), lat), lon), elev), cont), country),
          regions), muni), sched), gps), iata), local), home), wiki), keys)) =>
          Airport(id, ident, typ, name, lat, lon, elev, cont, country, regions, muni, sched,
                gps, iata, local, home, wiki, keys)
      }

      s.prepare(query).flatMap(_.stream((partialNameOrCode, partialNameOrCode), 64).compile.toList)
  }
  
  def getRunwaysByAirport(airportId: Int): IO[List[Runway]] = session.use { s =>
    val query: Query[Int, Runway] = sql"""
      SELECT id, airport_ref, airport_ident, length_ft, width_ft, surface, lighted,
            closed, le_ident, le_latitude_deg, le_longitude_deg, le_elevation_ft,
            le_heading_degT, le_displaced_threshold_ft, he_ident, he_latitude_deg,
            he_longitude_deg, he_elevation_ft, he_heading_degT, he_displaced_threshold_ft
      FROM runways WHERE airport_ref = $int4
    """.query(
      int4 ~ int4 ~ varchar ~ int4.opt ~ int4.opt ~ varchar.opt ~ bool ~
      bool ~ varchar.opt ~ float8.opt ~ float8.opt ~ int4.opt ~ float8.opt ~
      int4.opt ~ varchar.opt ~ float8.opt ~ float8.opt ~ int4.opt ~
      float8.opt ~ int4.opt
    ).map {
      case ((((((((((((((((((((id, ref), ident), len), width), surface), lit), closed), leId), 
        leLat), leLon), leElev), leHead), leThresh), heId), heLat), heLon), heElev), heHead), heThresh)) =>
        Runway(id, ref, ident, len, width, surface, lit, closed, leId, leLat, leLon,
              leElev, leHead, leThresh, heId, heLat, heLon, heElev, heHead, heThresh)
    }
    s.prepare(query).flatMap(_.stream(airportId, 64).compile.toList)
  }

  def getTopAirportCounts: IO[List[(String, Long)]] = session.use { s =>
    val query: Query[Void, (String, Long)] = sql"""
      SELECT c.name, COUNT(a.id) as airport_count
      FROM countries c
      LEFT JOIN airports a ON c.code = a.iso_country
      GROUP BY c.name
      ORDER BY airport_count DESC
      LIMIT 10
    """.query(varchar ~ int8).map { case (name, count) => (name, count) }
    
    s.prepare(query).flatMap(_.stream(Void, 64).compile.toList)
  }

  def getBottomAirportCounts: IO[List[(String, Long)]] = session.use { s =>
    val query: Query[Void, (String, Long)] = sql"""
      SELECT c.name, COUNT(a.id) as airport_count
      FROM countries c
      LEFT JOIN airports a ON c.code = a.iso_country
      GROUP BY c.name
      ORDER BY airport_count ASC
      LIMIT 10
    """.query(varchar ~ int8).map { case (name, count) => (name, count) }
    
    s.prepare(query).flatMap(_.stream(Void, 64).compile.toList)
  }

  def getRunwayTypesPerCountry: IO[List[(String, String, Long)]] = session.use { s =>
    val query: Query[Void, (String, String, Long)] = sql"""
      SELECT c.name, COALESCE(r.surface, 'Unknown'), COUNT(*) as surface_count
      FROM countries c
      JOIN airports a ON c.code = a.iso_country
      JOIN runways r ON a.id = r.airport_ref
      WHERE r.surface IS NOT NULL
      GROUP BY c.name, r.surface
      ORDER BY c.name, surface_count DESC
    """.query(varchar ~ varchar ~ int8)
      .map { case ((name, surface), count) => (name, surface, count) }
    
    s.prepare(query).flatMap(_.stream(Void, 64).compile.toList)
  }

  def getCommonRunwayLatitudes: IO[List[(String, Long)]] = session.use { s =>
    val query: Query[Void, (String, Long)] = sql"""
      SELECT le_ident, COUNT(*) as ident_count
      FROM runways
      WHERE le_ident IS NOT NULL
      GROUP BY le_ident
      ORDER BY ident_count DESC
      LIMIT 10
    """.query(varchar ~ int8).map { case (ident, count) => (ident, count) }
    
    s.prepare(query).flatMap(_.stream(Void, 64).compile.toList)
  }


  // GET STATS FOR API
  def getStats(countryCode: String): IO[Stats] = session.use { s =>
    val exactCodeQuery: Query[String, Stats] = sql"""
      WITH matching_countries AS (
        SELECT code
        FROM countries
        WHERE code ILIKE $varchar
        LIMIT 1
      ),
      country_stats AS (
        SELECT 
          c.code as country_code,
          CAST(COUNT(DISTINCT a.id) AS INTEGER) as airport_count,
          CAST(COUNT(DISTINCT r.id) AS INTEGER) as runway_count,
          CASE 
            WHEN COUNT(DISTINCT a.id) = 0 THEN 0.0
            ELSE CAST(COUNT(DISTINCT r.id) AS FLOAT) / CAST(COUNT(DISTINCT a.id) AS FLOAT)
          END as avg_runways_per_airport
        FROM matching_countries mc
        JOIN countries c ON c.code = mc.code
        LEFT JOIN airports a ON c.code = a.iso_country
        LEFT JOIN runways r ON a.id = r.airport_ref
        GROUP BY c.code
      )
      SELECT 
        country_code,
        airport_count,
        runway_count,
        avg_runways_per_airport
      FROM country_stats
    """.query(
      varchar ~ int4 ~ int4 ~ float8
    ).map {
      case (((code, airportCount), runwayCount), avgRunways) =>
        Stats(code, airportCount, runwayCount, avgRunways)
    }

    val nameSearchQuery: Query[String, Stats] = sql"""
      WITH matching_countries AS (
        SELECT code
        FROM countries
        WHERE name ILIKE '%' || $varchar || '%'
        LIMIT 1
      ),
      country_stats AS (
        SELECT 
          c.code as country_code,
          CAST(COUNT(DISTINCT a.id) AS INTEGER) as airport_count,
          CAST(COUNT(DISTINCT r.id) AS INTEGER) as runway_count,
          CASE 
            WHEN COUNT(DISTINCT a.id) = 0 THEN 0.0
            ELSE CAST(COUNT(DISTINCT r.id) AS FLOAT) / CAST(COUNT(DISTINCT a.id) AS FLOAT)
          END as avg_runways_per_airport
        FROM matching_countries mc
        JOIN countries c ON c.code = mc.code
        LEFT JOIN airports a ON c.code = a.iso_country
        LEFT JOIN runways r ON a.id = r.airport_ref
        GROUP BY c.code
      )
      SELECT 
        country_code,
        airport_count,
        runway_count,
        avg_runways_per_airport
      FROM country_stats
    """.query(
      varchar ~ int4 ~ int4 ~ float8
    ).map {
      case (((code, airportCount), runwayCount), avgRunways) =>
        Stats(code, airportCount, runwayCount, avgRunways)
    }

    // Choose query based on input length
    val query = if (countryCode.length == 2) {
      // For 2-character inputs, try exact code match first
      s.prepare(exactCodeQuery)
        .flatMap(_.option((countryCode)))
        .flatMap {
          case Some(stats) => IO.pure(stats)
          case None => 
            // Fall back to name search if no exact match found
            s.prepare(nameSearchQuery)
              .flatMap(_.option((countryCode)))
              .map(_.getOrElse(Stats(countryCode, 0, 0, 0.0)))
        }
    } else {
      // For other lengths, just use name search
      s.prepare(nameSearchQuery)
        .flatMap(_.option((countryCode)))
        .map(_.getOrElse(Stats(countryCode, 0, 0, 0.0)))
    }

    query
  }
}