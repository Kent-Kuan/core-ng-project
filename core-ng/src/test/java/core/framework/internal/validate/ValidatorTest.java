package core.framework.internal.validate;

import core.framework.api.validate.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author neo
 */
class ValidatorTest {
    @Test
    void validate() {
        var validator = Validator.of(TestBean.class);
        assertThatThrownBy(() -> validator.validate(null, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("bean must not be null");

        assertThatThrownBy(() -> validator.validate(new TestBean(), false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("value=field must not be null");
    }

    public static class TestBean {
        @NotNull
        public String value;
    }
}
