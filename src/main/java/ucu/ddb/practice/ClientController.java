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
            case "ex1_part1" -> runEx1(count, clients, "inc_p1", "count_p1", 1);
            case "ex1_part2" -> runEx1(count, clients, "inc_p2", "count_p2", 2);
            default -> """
                    Режим виконання заданий не правильно.
                    Має складатись з ex(номер завдання)_part(номер пункту завдання).
                    Наприклад: ex1_part2
                    """;
        };
    }

// Для завдання 1 --------------------------------------------------------------------------------------------

    @GetMapping("/run_ex1_part1")
    public String runTestEx1Part1Cli1(@RequestParam int clients) {
        return runEx1(10000, clients, "inc_p1", "count_p1", 1);
    }

    @GetMapping("/run_ex1_part2")
    public String runTestEx1Part1Cli2(@RequestParam int clients) {
        return runEx1(10000, clients, "inc_p2", "count_p2", 2);
    }

    private String runEx1(int count, int clients, String incCommand, String countCommand, int partNumber) {
        StringBuilder result = new StringBuilder(String.format("""
            Виконання тесту за логікою завдання 1, пункт %d з параметрами:
             - кількість викликів = %d
             - кількість клієнтів = %d
            
            Результати:
            """, partNumber, count, clients));
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

// Для завдання 2 --------------------------------------------------------------------------------------------

    @GetMapping("/run_ex2_part1")
    public String runTestEx2Part1() {
        return runEx2("inc_p1", 1);
    }

    @GetMapping("/run_ex2_part2")
    public String runTestEx2Part2() {
        return runEx2("inc_p2", 2);
    }

    @GetMapping("/run_ex2_part3")
    public String runTestEx2Part3() {
        return runEx2("inc_p3", 3);
    }

    @GetMapping("/run_ex2_part4")
    public String runTestEx2Part4() {
        return runEx2("inc_p4", 4);
    }

    @GetMapping("/run_ex2_part5")
    public String runTestEx2Part5() {
        return runEx2("inc_p5", 5);
    }

    @GetMapping("/run_ex2_part6")
    public String runTestEx2Part6() {
        restTemplate.postForEntity(getServerUrl() + "/" + "reset", null, String.class);
        return runEx1(10000, 10, "inc_p6", "count", 6);
    }

    private String runEx2(String incCommand, int partNumber) {
        StringBuilder result = new StringBuilder(String.format("""
            Виконання тесту за логікою завдання 2, пункт %d.
            
            Результати:
            """, partNumber));
        log.info(result.toString());
        restTemplate.postForEntity(getServerUrl() + "/" + "reset", null, String.class);

        final int clients = 10;
        final int iterations = 10000;
        CountDownLatch latch = new CountDownLatch(clients);
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= clients; i++) {
            final int clientId = i;
            executorService.submit(() -> {
                int successfulIncrements = 0;
                try {
                    while (successfulIncrements < iterations) {
                        try {
                            var response = restTemplate.postForEntity(getServerUrl() + "/" + incCommand,
                                    null, String.class
                            );

                            if (response.getStatusCode().is2xxSuccessful() && "OK".equals(response.getBody())) {
                                successfulIncrements++;
                            } else {
                                Thread.sleep(10);
                            }
                        } catch (Exception e) {
                            Thread.sleep(10);
                        }
                    }
                    String clientResult = String.format("Клієнт %d закінчив роботу. Успішно: %d", clientId, successfulIncrements);
                    synchronized (result) {
                        result.append("\n").append(clientResult);
                    }
                    log.info(clientResult);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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

        Integer finalCount = restTemplate.getForObject(getServerUrl() + "/" + "count", Integer.class);
        String counterResult = String.format("Фінальне значення лічильника = %d", finalCount);
        result.append("\n").append(counterResult);
        log.info(counterResult);

        long totalTimeMs = System.currentTimeMillis() - startTime;
        float totalTimeSec = (float) totalTimeMs / 1000;
        int totalRequests = 10000 * clients;
        int operationsPerSecond = totalTimeSec > 0 ? (int) (totalRequests / totalTimeSec) : 0;

        String timeResult = String.format("Час виконання = %.3f сек. Пропускна здатність = %d зап/сек", totalTimeSec, operationsPerSecond);
        result.append("\n").append(timeResult);
        log.info(timeResult);
        return result.toString();
    }

}