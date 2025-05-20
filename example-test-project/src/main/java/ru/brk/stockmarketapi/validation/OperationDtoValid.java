package ru.brk.stockmarketapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.brk.stockmarketapi.dto.OperationDto;

public class OperationDtoValid implements ConstraintValidator<OperationValidation, OperationDto> {
    @Override
    public void initialize(OperationValidation constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(OperationDto operationDto, ConstraintValidatorContext constraintValidatorContext) {
        return operationDto.getCurrencyId() != 1;
    }
}
