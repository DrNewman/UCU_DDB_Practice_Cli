package ucu.ddb.practice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ClientController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String SERVER_URL = "http://server-app:8080";

    @PostMapping("/run")
    public String runTest(@RequestParam int count, @RequestParam int clients) {
        // Тут ви зможете реалізувати логіку багатопоточного тестування
        return String.format("Starting test: count=%d, clients=%d. Target: %s",
                count, clients, SERVER_URL);
    }



}