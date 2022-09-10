package com.mario.secondkill.controller;


import com.mario.secondkill.entity.User;
import com.mario.secondkill.service.IOrderService;
import com.mario.secondkill.vo.OrderDetailVo;
import com.mario.secondkill.vo.RespBean;
import com.mario.secondkill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author hexiangdong
 * @since 2022-03-07
 */
@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private IOrderService orderService;

    //订单信息
    @RequestMapping("/detail")
    @ResponseBody
    public RespBean detail(User user, Long orderId) {
        if(user == null) {
            return RespBean.error(RespBeanEnum.USER_TIME_OUT);
        }

        OrderDetailVo detail = orderService.getDetail(orderId);

        return RespBean.success(detail);

    }

}
