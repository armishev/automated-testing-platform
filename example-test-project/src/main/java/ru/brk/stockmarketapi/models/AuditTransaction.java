package ru.brk.stockmarketapi.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Table(name = "trans_audit")
@Getter
@Setter
public class AuditTransaction {
    @Id
    private Long id;
    private Integer currencyId;
    private Long chatId;
    private LocalDateTime time;
    private String operation;
    private BigDecimal amount;

    public AuditTransaction(Long chatId, Integer currencyId,  String operation, BigDecimal amount) {
        this.chatId = chatId;
        this.currencyId = currencyId;
        this.operation = operation;
        this.amount = amount;
        this.time = LocalDateTime.now();
    }
}
