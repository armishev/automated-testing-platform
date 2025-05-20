package ru.brk.stockmarketapi.models;

import lombok.AllArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table
@Getter
@Setter
@AllArgsConstructor
public class Account {
    @Id
    private Long id;
    private Long chatId;
    private Integer currencyId;
    private BigDecimal balance;
}
