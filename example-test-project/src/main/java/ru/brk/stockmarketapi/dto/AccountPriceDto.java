package ru.brk.stockmarketapi.dto;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@AllArgsConstructor()
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountPriceDto {
    Integer currencyId;
    BigDecimal price;

}
