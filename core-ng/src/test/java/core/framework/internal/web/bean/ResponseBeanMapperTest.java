package core.framework.internal.web.bean;

import core.framework.internal.bean.BeanClassNameValidator;
import core.framework.internal.bean.TestBean;
import core.framework.internal.json.JSONMapper;
import core.framework.internal.validate.ValidationException;
import core.framework.util.Lists;
import core.framework.util.Strings;
import core.framework.util.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author neo
 */
class ResponseBeanMapperTest {
    private ResponseBeanMapper responseBeanMapper;
    private JSONMapper<TestBean> mapper;

    @BeforeEach
    void createResponseBeanMapper() {
        responseBeanMapper = new ResponseBeanMapper(new BeanMappers());
        responseBeanMapper.register(TestBean.class, new BeanClassNameValidator());
        mapper = new JSONMapper<>(TestBean.class);
    }

    @Test
    void validateList() {
        List<TestBean> list = Lists.newArrayList();
        assertThatThrownBy(() -> responseBeanMapper.toJSON(list))
            .isInstanceOf(Error.class)
            .hasMessageContaining("bean class must not be java built-in class");
    }

    @Test
    void toJSONWithEmptyOptional() {
        Optional<TestBean> optional = Optional.empty();
        byte[] bytes = responseBeanMapper.toJSON(optional);
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("null");
    }

    @Test
    void toJSONWithOptional() {
        var bean = new TestBean();
        bean.intField = 5;
        Optional<TestBean> optional = Optional.of(bean);
        byte[] bytes = responseBeanMapper.toJSON(optional);
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void toJSONWithValidationError() {
        assertThatThrownBy(() -> responseBeanMapper.toJSON(new TestBean()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void register() {
        responseBeanMapper.register(TestBean.class, new BeanClassNameValidator());
    }

    @Test
    void fromJSONWithEmptyOptional() {
        @SuppressWarnings("unchecked")
        var parsedBean = (Optional<TestBean>) responseBeanMapper.fromJSON(Types.optional(TestBean.class), Strings.bytes("null"));
        assertThat(parsedBean).isNotPresent();
    }

    @Test
    void fromJSONWithOptional() {
        var bean = new TestBean();
        bean.intField = 3;

        @SuppressWarnings("unchecked")
        var parsedBean = (Optional<TestBean>) responseBeanMapper.fromJSON(Types.optional(TestBean.class), mapper.toJSON(bean));
        assertThat(parsedBean).get().usingRecursiveComparison().isEqualTo(bean);
    }

    @Test
    void fromJSONWithValidationError() {
        var bean = new TestBean();

        assertThatThrownBy(() -> responseBeanMapper.fromJSON(TestBean.class, mapper.toJSON(bean)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("validation failed");
    }

    @Test
    void fromJSONWithVoid() {
        assertThat(responseBeanMapper.fromJSON(void.class, null)).isNull();
    }

    @Test
    void fromJSON() {
        var bean = new TestBean();
        bean.intField = 3;

        TestBean parsedBean = (TestBean) responseBeanMapper.fromJSON(TestBean.class, mapper.toJSON(bean));
        assertThat(parsedBean).usingRecursiveComparison().isEqualTo(bean);
    }

    @Test
    void withUnregisteredBean() {
        assertThatThrownBy(() -> responseBeanMapper.fromJSON(TestUnregisteredBean.class, new byte[0]))
                .isInstanceOf(Error.class)
                .hasMessageContaining("bean class is not registered");
    }

    public static class TestUnregisteredBean {
    }
}
