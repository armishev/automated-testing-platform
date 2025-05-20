package ru.brk.stockmarketapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PriceValidation implements ConstraintValidator<Price, Double> {
    @Override
    public void initialize(Price constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Double s, ConstraintValidatorContext constraintValidatorContext) {
        return s >= 0;
    }
}
