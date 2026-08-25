package solutis.lucas.afonso.helpdesk.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(@Value("${helpdesk.user-service-url}") String userServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    public boolean existsById(Long id) {
        HttpStatusCode status = restClient.get()
                .uri("/users/{id}", id)
                .exchange((request, response) -> response.getStatusCode());

        return status.is2xxSuccessful();
    }
}