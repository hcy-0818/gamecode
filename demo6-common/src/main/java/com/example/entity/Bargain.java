package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bargain")
public class Bargain {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal buyerPrice;
    private BigDecimal sellerPrice;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
