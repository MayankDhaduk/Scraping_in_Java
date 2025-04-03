# Chess Tournament Data Scraper (Java)

## Overview
This Java-based scraper extracts chess tournament data from "The Week in Chess" (TWIC) and Chess-Results, downloading PGN (Portable Game Notation) files and tournament results. The collected data is saved as a JSON file for further analysis.

## Features
- Extracts tournament details such as event name, date, rounds, and type.
- Downloads PGN files containing chess games.
- Scrapes results from Chess-Results when available.
- Stores all the data in `combined_chess_data.json` in a structured format.
- Uses multithreading to speed up data extraction and downloads.

## Requirements
Before running the script, ensure you have the following installed:

- **Java 8+**
- **Maven or Gradle (optional for dependency management)**

Install the required dependencies using Maven or manually add them to your project:

```xml
<dependencies>
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.8.3</version>
    </dependency>
    <dependency>
        <groupId>io.github.bonigarcia</groupId>
        <artifactId>webdrivermanager</artifactId>
        <version>5.4.1</version>
    </dependency>
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.15.3</version>
    </dependency>
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
</dependencies>
```

## How It Works

### 1. Setting Up Selenium WebDriver
- The script initializes a **headless Chrome browser** to navigate the TWIC website.
- A **WebDriverWait** is used to ensure elements are loaded before interaction.

### 2. Extracting Tournament Data
- The script finds the navigation menu and retrieves the link to **"A Year of PGN Game Files."**
- It navigates to the page and extracts data from tournament tables:
  - **Event name**
  - **Dates**
  - **Results link**
  - **Rounds**
  - **Type**
  - **PGN download links**
- PGN files are downloaded concurrently using a **thread pool**.

### 3. Scraping Chess-Results for Tournament Results
- If a **results link** exists, the script extracts **top 5 rows** from the results table.
- This data is mapped to the corresponding tournament.

### 4. Saving Data to JSON
- Extracted data is formatted and saved into `combined_chess_data.json` using **Gson**.

## Data Structure
The final JSON output has the following format:

```json
{
    "TWIC_Data": [
        {
            "EVENT": "57th_Biel_GM1-960_2024",
            "DATES": "13.07.2024-26.07.2024",
            "RES": "https://chess-results.com/example",
            "RDS": "7",
            "TYPE": "Swiss",
            "PGN": ["https://theweekinchess.com/assets/files/pgn/example.pgn"],
            "Results": [
                ["1", "Player A", "Country", "Score"],
                ["2", "Player B", "Country", "Score"]
            ]
        }
    ]
}
```

## Running the Script
Compile and run the program using the following commands:

```sh
javac -cp ".;lib/*" Demo.java
java -cp ".;lib/*" Demo
```

If using Maven, simply run:

```sh
mvn compile exec:java -Dexec.mainClass="Demo"
```

## Error Handling
- If a PGN file cannot be downloaded (e.g., **404 error**), the script logs the failure.
- If a results table is not found on Chess-Results, it prints an error message.
- Uses **timeout handling** to prevent infinite waiting for a webpage.

## Notes
- Some PGN files may not be available due to website restrictions.
- Chess-Results pages may have different structures, requiring occasional updates.

## Conclusion
This Java scraper efficiently collects chess tournament data, making it easy to access PGN files and tournament results. By utilizing Selenium and Jsoup, it automates the tedious process of gathering chess-related data.

