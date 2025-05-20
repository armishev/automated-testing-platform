package ru.brk.stockmarketapi.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

@Table
@Getter
@Setter
@AllArgsConstructor
public class EverySecondPrices {
    private Integer currencyId;
    private Double price;
}
