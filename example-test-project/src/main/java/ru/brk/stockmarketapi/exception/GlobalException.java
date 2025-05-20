package ru.brk.stockmarketapi.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleCustomTradeException(DataIntegrityViolationException ex) {
        return ResponseEntity.status(400).body(Map.of("message", "Произошла ошибка во время операции, повторите попытку"));
    }

    @ExceptionHandler({OperationException.class, TradeRendererException.class})
    public ResponseEntity<Map<String, String>> handleCustomTradeException(RuntimeException ex) {
        return ResponseEntity.status(500).body(Map.of("message", ex.getMessage()));
    }




}
