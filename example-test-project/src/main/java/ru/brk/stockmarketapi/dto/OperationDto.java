package ru.brk.stockmarketapi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.brk.stockmarketapi.validation.Price;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OperationDto {

    @NotNull
    Long userId;
    @NotNull
    Integer currencyId;
//    @Price(message = "кол-во не может быть отрицательным")
    BigDecimal symbolQuantity;
}
