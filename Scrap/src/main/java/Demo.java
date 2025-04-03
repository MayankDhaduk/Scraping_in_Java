import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class Demo {

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments(
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        // Step 1: Open main page
        driver.get("https://theweekinchess.com");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("navigation1")));

        // Step 2: Extract the correct link
        WebElement navMenu = driver.findElement(By.className("navigation1"));
        WebElement targetLink = navMenu.findElement(By.xpath(".//a[contains(@href, 'a-year-of-pgn-game-files')]"));
        String twicUrl = targetLink.getAttribute("href");

        // Step 3: Navigate to the extracted link
        driver.get(twicUrl);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));

        String saveDirectory = "C:/Users/ADMIN/Downloads/Scrape-store-java";
        new File(saveDirectory).mkdirs();

        List<Map<String, Object>> extractedData = Collections.synchronizedList(new ArrayList<>());
        List<WebElement> tables = driver.findElements(By.tagName("table"));
        List<String> resLinks = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (WebElement table : tables) {
            for (WebElement row : table.findElements(By.tagName("tr"))) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() >= 10) {
                    String event = cols.get(0).getText().trim().replace(" ", "_");
                    String date = cols.get(3).getText().trim().replace(" ", "_");
                    String res = cols.get(5).findElements(By.tagName("a")).isEmpty() ? "No Link"
                            : cols.get(5).findElement(By.tagName("a")).getAttribute("href");
                    String rds = cols.get(7).getText().trim();
                    String typeField = cols.get(8).getText().trim();
                    WebElement pgnColumn = cols.get(9);

                    Map<String, Object> rowData = new ConcurrentHashMap<>();
                    rowData.put("EVENT", event);
                    rowData.put("DATES", date);
                    rowData.put("RES", res);
                    rowData.put("RDS", rds);
                    rowData.put("TYPE", typeField);
                    rowData.put("PGN", Collections.synchronizedList(new ArrayList<String>()));
                    rowData.put("Results", Collections.synchronizedList(new ArrayList<>()));

                    if (res.startsWith("http")) resLinks.add(res);

                    for (WebElement pgnLink : pgnColumn.findElements(By.tagName("a"))) {
                        String pgnUrl = pgnLink.getAttribute("href");
                        if (pgnUrl != null && pgnUrl.startsWith("http")) {
                            ((List<String>) rowData.get("PGN")).add(pgnUrl);
                            executor.submit(() -> {
                                try (InputStream in = new URL(pgnUrl).openStream()) {
                                    Path filePath = Paths.get(saveDirectory, event + "_" + date + ".pgn");
                                    Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                                    System.out.println("Downloaded: " + filePath.getFileName());
                                } catch (IOException e) {
                                    System.out.println("Failed to download PGN: " + pgnUrl);
                                }
                            });
                        }
                    }
                    extractedData.add(rowData);
                }
            }
        }

        Map<String, List<List<String>>> resultsMapping = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        for (String resUrl : resLinks) {
            futures.add(executor.submit(() -> {
                try {
                    Document doc = Jsoup.connect(resUrl).userAgent("Mozilla/5.0").timeout(60000).get();
                    Element targetTable = doc.selectFirst("table.CRs1");
                    if (targetTable != null) {
                        List<List<String>> results = new ArrayList<>();
                        Elements rows = targetTable.select("tr");
                        for (int i = 1; i < Math.min(6, rows.size()); i++) {
                            Elements cells = rows.get(i).select("td");
                            List<String> cellData = new ArrayList<>();
                            for (Element cell : cells) cellData.add(cell.text());
                            results.add(cellData);
                        }
                        resultsMapping.put(resUrl, results);
                    }
                } catch (IOException e) {
                    System.out.println("Failed to fetch results from: " + resUrl);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        for (Map<String, Object> entry : extractedData) {
            String resUrl = (String) entry.get("RES");
            if (resultsMapping.containsKey(resUrl)) {
                entry.put("Results", resultsMapping.get(resUrl));
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("combined_chess_data.json"))) {
            writer.write(new com.google.gson.GsonBuilder().setPrettyPrinting().create()
                    .toJson(Collections.singletonMap("TWIC_Data", extractedData)));
            System.out.println("Data saved to combined_chess_data.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        driver.quit();
        System.out.println("All data has been successfully scraped and stored.");
    }
}
