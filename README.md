# Airport Information System (Scala)
## M1-BDML1 - EFREI Paris - 12/2024
## Students: Anonymous (Lxmune), Antoine COMBALDIEU (AntoineCmbld)

This is a Scala command-line application that parses CSV data about countries, airports, and runways. It allows users to query airports and runways by country (via country code or name) and generate various reports.

### Features:
- Parse CSV files to create case class instances for countries, airports, and runways.
- Query airports and runways by country code or name.
- Generate reports such as:
  - Top 10 countries with the most/least airports.
  - Runway types per country.
  - Most common runway identifiers.

### Requirements:
- Scala 3.6 or later
- SBT (Scala Build Tool)
- Docker + Compose

### Running the Project:

1. **Set up the PostgreSQL database:**
    - Ensure Docker is installed and running.
    - Use command : `sudo ./launch-services.sh` on Ubuntu
                    `./launch-services.sh` on Windows
    - If there is a permission error, use `chmod +x launch-services.sh` to make the script executable on Linux.

2. **Run the project:**
    - Use command : `sbt run`
    It may take around 5 to 10 minutes for your computer to fully populate the database.

3. **Access the API:**
    - The server will start on `http://localhost:8080` (configurable).
    - Available endpoints:
        - `GET /top-airports` - Retrieve the top 10 countries with the most airports.
        - `GET /bottom-airports` - Retrieve the top 10 countries with the least airports.
        - `GET /runway-types` - Retrieve the runway types per country.
        - `GET /common-latitudes` - Retrieve the most common runway latitudes.
        - `GET /countries` - Retrieve all countries.
        - `GET /query/{countryCode}` - Retrieve airports and runways for a country.
        - `GET /airports/{countryCode}` - Retrieve all airports for a country.
        - `GET /runways/{airportId}` - Retrieve all runways for an airport.
        - `GET /stats/{countryCode}` - Retrieve statistics for a country.

    - Examples:
        - `GET http://localhost:8080/top-airports`
        - `GET http://localhost:8080/bottom-airports`
        - `GET http://localhost:8080/runway-types`
        - `GET http://localhost:8080/common-latitudes`
        - `GET http://localhost:8080/countries`
        - `GET http://localhost:8080/query/FR`
        - `GET http://localhost:8080/airports/FR`
        - `GET http://localhost:8080/runways/3578`
        - `GET http://localhost:8080/stats/FR`


4. **User Interface:**
    - When compiling and running the project, the user is going to choose between the API (1) and the CLI (2). The API is going to be available on `http://localhost:8080` and the CLI is going to be available in the terminal. 
    - When choosing the API, the user is going to be able to use the endpoints mentioned above.
    - And when choosing the CLI, the user is going to be able to interact with the program in the terminal, by choosing the Query menu or the Reports menu.
    - The Query menu is going to ask the user to enter a country code or a country name (even incomplete), and then it is going to display the airports and runways of the country (the list can be long).
    - The Reports menu is going to ask the user to choose between the Top 10 countries with the most/least airports, the Runway types per country, and the Most common runway identifiers.
    - To exit the program, the user can choose the Exit option in the main menu.