package ucu.ddb.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class ClientController {
    private static final int MAX_CLIENTS = 10;

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${SERVER_HOST:${server.host}}")
    private String serverHost;

    private String getServerUrl() {
        return "http://" + serverHost + ":8080";
    }

    @GetMapping("/run")
    public String runTest(@RequestParam String mode, @RequestParam int count, @RequestParam int clients) {
        return switch (mode) {
            case "ex1_part1" -> run(count, clients, "inc_p1", "count_p1", 1, 1);
            case "ex1_part2" -> run(count, clients, "inc_p2", "count_p2", 1, 2);
            default -> """
                    Режим виконання заданий не правильно.
                    Має складатись з ex(номер завдання)_part(номер пункту завдання).
                    Наприклад: ex1_part2
                    """;
        };
    }

    @GetMapping("/run_ex1_part1")
    public String runTestEx1Part1Cli1(@RequestParam int clients) {
        return run(10000, clients, "inc_p1", "count_p1", 1, 1);
    }

    @GetMapping("/run_ex1_part2")
    public String runTestEx1Part1Cli2(@RequestParam int clients) {
        return run(10000, clients, "inc_p2", "count_p2", 1, 2);
    }

    private String run(int count, int clients, String incCommand, String countCommand,
                       int exerciseNumber, int partNumber) {
        StringBuilder result = new StringBuilder(String.format("""
            Виконання тесту за логікою завдання %d, пункт %d з параметрами:
             - кількість викликів = %d
             - кількість клієнтів = %d
            
            Результати:
            """, exerciseNumber, partNumber, count, clients));
        log.info(result.toString());

        CountDownLatch latch = new CountDownLatch(clients);
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= clients; i++) {
            final int clientId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < count; j++) {
                        restTemplate.postForEntity(getServerUrl() + "/" + incCommand, null, String.class);
                    }
                    Integer finalCount = restTemplate.getForObject(getServerUrl() + "/" + countCommand, Integer.class);

                    String clientResult = String.format("Клієнт %d закінчив роботу. Current server count = %d", clientId, finalCount);
                    synchronized (result) {
                        result.append("\n").append(clientResult);
                    }
                    log.info(clientResult);
                } catch (Exception e) {
                    log.error("Помилка у потоці клієнта " + clientId, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Виконання було перервано";
        }

        long totalTimeMs = System.currentTimeMillis() - startTime;
        float totalTimeSec = (float) totalTimeMs / 1000;
        int totalRequests = count * clients;
        int operationsPerSecond = totalTimeSec > 0 ? (int) (totalRequests / totalTimeSec) : 0;

        String summary = String.format("Час виконання = %.3f сек. Пропускна здатність = %d зап/сек", totalTimeSec, operationsPerSecond);
        result.append("\n").append(summary);
        log.info(summary);
        return result.toString();
    }

}