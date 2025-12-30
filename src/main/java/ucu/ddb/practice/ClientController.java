package ucu.ddb.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class ClientController {

    private static final String SERVER_URL = "http://localhost:8080";
//    private static final String SERVER_URL = "http://server-app:8080";
    private static final int MAX_CLIENTS = 10;

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/run")
    public String runTest(@RequestParam String mode, @RequestParam int count, @RequestParam int clients) {
        return switch (mode) {
            case "ex1_part1" -> runEx1Part1(count, clients);
            case "ex1_part2" -> runEx1Part2(count, clients);
            default -> """
                    Режим виконання заданий не правильно.
                    Має складатись з ex(номер завдання)_part(номер пункту завдання).
                    Наприклад: ex1_part2
                    """;
        };
    }

    @GetMapping("/run_ex1_part1")
    public String runTestEx1Part1Cli1(@RequestParam int clients) {
        return runEx1Part1(10000, clients);
    }

    @GetMapping("/run_ex1_part2")
    public String runTestEx1Part1Cli2(@RequestParam int clients) {
        return runEx1Part2(10000, clients);
    }

    private String runEx1Part1(int count, int clients) {
        StringBuilder result = new StringBuilder(String.format("""
            Виконання тесту за логікою завдання 1, пункт 1 з параметрами:
             - кількість викликів = %d
             - кількість клієнтів = %d
            
            Результати:
            """, count, clients));
        log.info(result.toString());

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < clients; i++) {
            // починаючи звідси
            for (int j = 0; j < count; j++) {
                restTemplate.postForEntity(SERVER_URL + "/inc_p1", null, String.class);
            }
            Integer finalCount = restTemplate.getForObject(SERVER_URL + "/count_p1", Integer.class);
            String clientResult = String.format("Клієнт %d закінчив роботу. \"\\count\" = %d", (i + 1), finalCount);
            result.append("\n").append(clientResult);
            log.info(clientResult);
            // закінчуючи тут, це блок має виконуватись у окремому потоці
        }
        // все що нижче має виконуватись тільки після того, як усі потоки клієнтів відпрацюють
        long totalTimeMs = System.currentTimeMillis() - startTime;
        float totalTimeSec = (float) totalTimeMs / 1000;
        int operationsPerSecond = (int) (count * clients / totalTimeSec);
        String summary = String.format("Час виконання = %f сек. кількість запитів за секунду = %d", totalTimeSec, operationsPerSecond);
        result.append("\n").append(summary);
        log.info(summary);
        return result.toString();
    }

    private String runEx1Part2(int count, int clients) {
        return "Temporary stub";
    }

}