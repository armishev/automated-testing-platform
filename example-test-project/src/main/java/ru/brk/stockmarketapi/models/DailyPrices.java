package ru.brk.stockmarketapi.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Table
@Getter
@Setter
@AllArgsConstructor
public class DailyPrices {
    private Integer currencyId;
    private Double price;
    private LocalDate daily;
}
