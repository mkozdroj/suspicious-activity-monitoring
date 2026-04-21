package com.grad.sam.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationAdapter;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private static Level originalHandlerLogLevel;

    @BeforeAll
    static void silenceHandlerLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        originalHandlerLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    @AfterAll
    static void restoreHandlerLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logger.setLevel(originalHandlerLogLevel);
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnNotFoundForDataNotFoundException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("missing"));
    }

    @Test
    void shouldReturnBadRequestForInvalidInputException() throws Exception {
        mockMvc.perform(get("/test/invalid-input"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void shouldReturnConflictForBusinessConflictException() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_CONFLICT"))
                .andExpect(jsonPath("$.message").value("duplicate"));
    }

    @Test
    void shouldReturnBadRequestForIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("invalid argument"));
    }

    @Test
    void shouldReturnInternalServerErrorForIllegalStateException() throws Exception {
        mockMvc.perform(get("/test/illegal-state"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @Test
    void shouldReturnValidationDetailsForRequestBodyValidation() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0]").value("name: must not be blank"));
    }

    @Test
    void shouldReturnTypeMismatchForWrongParameterType() throws Exception {
        mockMvc.perform(get("/test/type/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    @Test
    void shouldReturnValidationFailedForConstraintViolationException() throws Exception {
        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Parameter validation failed"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0]").exists());
    }

    @Test
    void shouldReturnValidationFailedForHandlerMethodValidationException() throws Exception {
        mockMvc.perform(get("/test/handler-method-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Parameter validation failed"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0]").exists());
    }

    @RestController
    @Validated
    static class TestController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new DataNotFoundException("missing");
        }

        @GetMapping("/test/invalid-input")
        String invalidInput() {
            throw new InvalidInputException("bad input");
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new BusinessConflictException("duplicate");
        }

        @GetMapping("/test/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("invalid argument");
        }

        @GetMapping("/test/illegal-state")
        String illegalState() {
            throw new IllegalStateException("broken state");
        }

        @PostMapping("/test/body")
        String body(@Valid @RequestBody Payload payload) {
            return payload.name();
        }

        @GetMapping("/test/param")
        String param(@RequestParam @Positive Integer id) {
            return String.valueOf(id);
        }

        @GetMapping("/test/type/{id}")
        String type(@PathVariable Integer id) {
            return String.valueOf(id);
        }

        @GetMapping("/test/constraint")
        String constraint() {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
            Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload(""));
            throw new ConstraintViolationException(violations);
        }

        @GetMapping("/test/handler-method-validation")
        String handlerMethodValidation() throws NoSuchMethodException {
            Method method = TestController.class.getDeclaredMethod("param", Integer.class);
            MethodValidationResult result = new MethodValidationAdapter()
                    .validateArguments(this, method, null, new Object[]{-1}, new Class<?>[0]);
            throw new HandlerMethodValidationException(result);
        }
    }

    record Payload(@NotBlank String name) {
    }
}
