package core.log.web;

import core.framework.web.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author neo
 */
class SendEventRequestValidatorTest {
    private SendEventRequestValidator validator;

    @BeforeEach
    void createCollectEventRequestValidator() {
        validator = new SendEventRequestValidator();
    }

    @Test
    void validate() {
        assertThatThrownBy(() -> validator.validate(request(SendEventRequest.Result.OK, null, null)))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> validator.validate(request(SendEventRequest.Result.WARN, null, null)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate(request(SendEventRequest.Result.ERROR, null, null)))
                .isInstanceOf(BadRequestException.class);

        validator.validate(request(SendEventRequest.Result.OK, "action", null));
        validator.validate(request(SendEventRequest.Result.WARN, null, "ERROR_CODE"));
        validator.validate(request(SendEventRequest.Result.ERROR, null, "ERROR_CODE"));
    }

    @Test
    void validateContext() {
        assertThatThrownBy(() -> validator.validateContext(Map.of("context", "12345"), 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void validateInfo() {
        assertThatThrownBy(() -> validator.validateInfo(Map.of("key", "value"), 7))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too long");
    }

    private SendEventRequest request(SendEventRequest.Result result, String action, String errorCode) {
        SendEventRequest request = new SendEventRequest();
        var event = new SendEventRequest.Event();
        event.result = result;
        event.action = action;
        event.errorCode = errorCode;
        request.events.add(event);
        return request;
    }
}
