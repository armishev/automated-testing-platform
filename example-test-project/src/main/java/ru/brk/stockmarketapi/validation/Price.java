package ru.brk.stockmarketapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PriceValidation.class)
public @interface Price {


    String message() default "Некорректное значение цены";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

