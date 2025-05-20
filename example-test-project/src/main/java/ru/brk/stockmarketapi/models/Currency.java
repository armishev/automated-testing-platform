package ru.brk.stockmarketapi.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

@Table
@Getter
@Setter
@AllArgsConstructor
public class Currency {
    private String name;
    private Integer currencyId;
}
