# AuthoMate

AuthoMate is a Spring Boot web application that automates data entry into the
configured PGS website using Selenium. It provides a browser-based interface
for validating Excel workbooks, running automation, and monitoring its status.

## Features

- Validate Excel workbooks for the farmer-data workflow.
- Validate workbooks for the latitude/longitude update workflow.
- Run and stop Selenium automation from the web interface.
- Stream automation status and display logs.

## Requirements

- Java 17 or later.
- Google Chrome. The application uses WebDriverManager to set up ChromeDriver.
- Network access to the configured PGS website and to download the matching
  ChromeDriver when needed.

## Run locally

Start the application with the Maven wrapper:

```bash
./mvnw spring-boot:run
```

On Windows, use:

```bat
mvnw.cmd spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080). To build and test:

```bash
./mvnw test
```

## Excel workbooks

Upload `.xlsx` files through the web interface and validate them before
starting an automation run. The workbook's first sheet must contain a header
row.

The farmer-data workflow requires these columns:

- `Farmer Name`, `Village`, `Pincode`, `Gender`, `Category`, `Total Area`,
  `Organic Area`
- At least one of `Fathers Name` or `Fathers Name/ Husband Name`
- At least one of `Age` or `Farmer Age`
- At least one of `Khata No.` or `Khasra No.`
- At least one of `Plot No.` or `Plot No`

The latitude/longitude workflow requires `FarmerName (Original)`, `Village`,
`Longitude`, `Latitude`, and `Member ID`.

The application accepts uploads up to 50 MB. Use the interface's validation
results to identify missing columns or invalid rows before running automation.

## Configuration

The default PGS login URL is configured with `app.url` in
`src/main/resources/application.properties`. Override it at runtime with a
Spring property or environment-specific configuration when appropriate.
Application logs are written to `~/AuthoMate/logs/authomate.log` by default.

## HTTP endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/` | Web interface |
| `GET` | `/api/health` | Health check |
| `POST` | `/validate` | Validate a farmer-data `.xlsx` workbook |
| `POST` | `/validateLatLong` | Validate a latitude/longitude `.xlsx` workbook |
| `POST` | `/start`, `/start2` | Start an automation workflow |
| `POST` | `/stop` | Stop automation |
| `GET` | `/status/stream` | Stream automation status |
| `GET` | `/logs` | Read application automation logs |
| `POST` | `/app/shutdown` | Shut down the application |

The upload and automation endpoints are used by the web interface. For request
fields and parameters, refer to the controller implementation.
