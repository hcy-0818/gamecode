package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("registration")
public class Registration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long accountId;
    private String gameName;
    private String server;
    private String accountLevel;
    private Integer skinCount;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String description;
    private Integer allowBargain;
    private String images;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
