package ru.brk.stockmarketapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OperationDtoValid.class)
public @interface OperationValidation {

    String message() default "Нельзя покупать одно и то же";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
