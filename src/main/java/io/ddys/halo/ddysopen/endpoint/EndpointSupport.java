package io.ddys.halo.ddysopen.endpoint;

import io.ddys.halo.ddysopen.api.DdysApiClient;
import io.ddys.halo.ddysopen.api.DdysApiException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

final class EndpointSupport {

    private EndpointSupport() {
    }

    static Map<String, Object> query(ServerRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        request.queryParams().forEach((key, raw) -> {
            if (key == null || key.isBlank() || raw == null || raw.isEmpty()) {
                return;
            }
            if (raw.size() == 1) {
                String first = raw.get(0);
                if (first != null && !first.isBlank()) {
                    values.put(key, first);
                }
            } else {
                values.put(key, raw.stream().filter(item -> item != null && !item.isBlank()).toList());
            }
        });
        return values;
    }

    static Mono<ServerResponse> ok(Object body) {
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body);
    }

    static Mono<ServerResponse> error(Throwable throwable) {
        int status = throwable instanceof DdysApiException apiException
            ? apiException.getStatus()
            : throwable instanceof IllegalArgumentException
            ? HttpStatus.BAD_REQUEST.value()
            : HttpStatus.BAD_GATEWAY.value();
        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(DdysApiClient.responseError(throwable));
    }
}
