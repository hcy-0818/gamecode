package com.example.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BargainDTO {
    private Long id;
    private Long accountId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal buyerPrice;
    private BigDecimal sellerPrice;
    private String status;
    private String gameName;
    private String accountLevel;
    private BigDecimal originalPrice;
    private LocalDateTime createTime;
}
