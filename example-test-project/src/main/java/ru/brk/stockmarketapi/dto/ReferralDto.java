package ru.brk.stockmarketapi.dto;


import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralDto {
    Long referrerId;
    Long referralId;
}
