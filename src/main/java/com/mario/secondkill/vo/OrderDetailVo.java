package com.mario.secondkill.vo;

import com.mario.secondkill.entity.Order;
import lombok.Data;

@Data
public class OrderDetailVo {

    private Order order;
    private GoodsVo goodsVo;
}
