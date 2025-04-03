import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ResultPlayerScrap {

	public static void main(String[] args) {
		// Setup Chrome WebDriver
		WebDriverManager.chromedriver().setup();
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless"); // Run headless mode
		WebDriver driver = new ChromeDriver(options);

		// Open the webpage
		String url = "https://chess-results.com/tnr1080156.aspx";
		driver.get(url);

		// Wait until the table is loaded
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.className("CRs1")));

		// Get page source and parse with JSoup
		Document doc = Jsoup.parse(driver.getPageSource());

		// Find the table with class "CRs1"
		Element table = doc.selectFirst("table.CRs1");

		List<List<String>> data = new ArrayList<>();

		if (table != null) {
			Elements rows = table.select("tr");

			// Extract first 5 rows (excluding header)
			for (int i = 1; i <= Math.min(5, rows.size() - 1); i++) {
				Element row = rows.get(i);
				Elements cells = row.select("td");
				List<String> rowData = new ArrayList<>();

				for (Element cell : cells) {
					rowData.add(cell.text().trim());
				}

				if (!rowData.isEmpty()) {
					data.add(rowData);
				}
			}
		} else {
			System.out.println("Table with class 'CRs1' not found.");
		}

		// Save data to JSON
		String jsonFilename = "chess_results.json";
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFilename), data);
			System.out.println("Data successfully saved to " + jsonFilename);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Close the browser
		driver.quit();
	}

}
