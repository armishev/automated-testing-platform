package ru.brk.stockmarketapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.brk.stockmarketapi.dto.OperationDto;
import ru.brk.stockmarketapi.dto.ReferralDto;
import ru.brk.stockmarketapi.dto.TradeDto;
import ru.brk.stockmarketapi.models.Account;
import ru.brk.stockmarketapi.models.Currency;
import ru.brk.stockmarketapi.repo.CurrencyRepo;
import ru.brk.stockmarketapi.validation.OperationValidation;
import ru.brk.stockmarketapi.service.MarketService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarketController {


    private final MarketService marketService;

    @GetMapping("/trade/{id}")
    public Mono<TradeDto> trade(@PathVariable Long id, @RequestParam Integer currencyId, @RequestParam String operation) {
        return marketService.tradeRender(id, currencyId, operation);
    }

    @PostMapping("/sell")
    public Mono<Map<String, BigDecimal>> sell(@Validated @OperationValidation @RequestBody OperationDto operationDto) {
        return marketService.sell(operationDto);
    }

    @PostMapping("/buy")
    public Mono<Map<String, BigDecimal>> buy(  @RequestBody OperationDto operationDto) {
        return marketService.buy(operationDto);
    }

    @PostMapping("/reward")
    public Mono<Account> reward(@Validated @RequestBody OperationDto operationDto) {
        return marketService.reward(operationDto);
    }

    @GetMapping("/update")
    public void update() {
         marketService.initCurrency();
    }





}
