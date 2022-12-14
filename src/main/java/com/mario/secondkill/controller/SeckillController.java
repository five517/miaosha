package com.mario.secondkill.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mario.secondkill.config.AccessLimit;
import com.mario.secondkill.entity.Order;
import com.mario.secondkill.entity.SeckillMessage;
import com.mario.secondkill.entity.SeckillOrder;
import com.mario.secondkill.entity.User;
import com.mario.secondkill.exception.GlobalException;
import com.mario.secondkill.rabbitmq.MQSender;
import com.mario.secondkill.service.IGoodsService;
import com.mario.secondkill.service.IOrderService;
import com.mario.secondkill.service.ISeckillOrderService;
import com.mario.secondkill.vo.GoodsVo;
import com.mario.secondkill.vo.RespBean;
import com.mario.secondkill.vo.RespBeanEnum;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private RedisScript<Long> script;

    private Map<Long, Boolean> emptyMap = new HashMap<>();

    /*
    windows?????????QPS 785.9
            ?????????QPS 1350
            ?????????QPS 2450
    linux?????????QPS 170
     */
    @RequestMapping("/doSeckill2")
    public String doSeckill2(Model model, User user, Long goodsId) {
        if(user == null) {
            return "login";
        }
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        model.addAttribute("goods", goodsVo);
        //????????????
        if(goodsVo.getStockCount() <= 0) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        //???????????????????????????????????????
        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        wrapper.eq("goods_id", goodsId);
        SeckillOrder seckillOrder = seckillOrderService.getOne(wrapper);
        if(seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.HAS_SECKILL.getMessage());
            return "secKillFail";
        }
        //?????????
        Order order = orderService.seckill(user,goodsVo);
        model.addAttribute("order", order);
        return "orderDetail";
    }

    //??????????????????
    @RequestMapping("/{path}/doSeckill")
    @ResponseBody
    public RespBean doSeckill(@PathVariable String path, User user, Long goodsId) {
        if(user == null) {
            return RespBean.error(RespBeanEnum.USER_TIME_OUT);
        }

        ValueOperations valueOperations = redisTemplate.opsForValue();
        Boolean check = orderService.checkPath(user, path, goodsId);
        if(!check) {
            return RespBean.error(RespBeanEnum.PATH_ERROR);
        }
        //??????????????????????????????????????? ????????????
        SeckillOrder seckillOrder = (SeckillOrder)valueOperations.get("order:"+user.getId()+":"+goodsId);
        if(seckillOrder != null) {
            return RespBean.error(RespBeanEnum.HAS_SECKILL);
        }
        //????????????????????????redis?????????
        if(emptyMap.get(goodsId)) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //redis????????????
//        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
        //??????lua??????
        Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
        if(stock == 0) {
            emptyMap.put(goodsId, true);
            //valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //?????????
        SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
        mqSender.sendSeckillMessage(JSON.toJSONString(seckillMessage));
        return RespBean.success(0);
    }

    //??????????????????????????????????????????????????????redis
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsVos = goodsService.getGoodsVo();
        if(goodsVos.size() == 0) {
            return ;
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        for(GoodsVo goodsVo : goodsVos) {
            valueOperations.set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
            emptyMap.put(goodsVo.getId(), false);
        }
    }

    //???????????????????????????
    /*
    orderId??????
    -1 ????????????
    0 ?????????
     */
    @RequestMapping("/result")
    @ResponseBody
    public RespBean getResult(User user, String goodsId) {
        if(user == null) {
            return RespBean.error(RespBeanEnum.USER_TIME_OUT);
        }
        String key = "order" + user.getId() + ":" + goodsId;
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Long orderId = -1l;
        if(valueOperations.get(key) != null) {
            SeckillOrder seckillOrder = (SeckillOrder) valueOperations.get(key);
            orderId = seckillOrder.getOrderId();
        } else if((int)valueOperations.get("seckillGoods:" + goodsId) <= 0) {
            orderId = -1l;
        } else {
            orderId = 0l;
        }
        return RespBean.success(orderId);
    }

    //?????????????????????
    @RequestMapping("/path")
    @ResponseBody
    @AccessLimit(second=5,maxCount=5,needLogin=true)
    public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request) {
        if(user == null) {
            return RespBean.error(RespBeanEnum.USER_TIME_OUT);
        }

        //???????????????????????????
        Boolean check = orderService.checkCptcha(user, goodsId, captcha);
        if(!check) {
            return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
        }

        //?????????????????????
        String str = orderService.createPath(user, goodsId);
        return RespBean.success(str);
    }

    //???????????????
    @RequestMapping("/captcha")
    public void vertifyCode(User user, Long goodsId, HttpServletResponse response) {
        if(user == null || goodsId < 0) {
            throw new GlobalException(RespBeanEnum.ERROR);
        }

        //????????????????????????????????????
        response.setContentType("image/jpg");
        response.setHeader("Pargam","No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        //?????????????????????????????????redis???
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
        redisTemplate.opsForValue().set("captcha:" + user.getId() + ":" + goodsId, captcha.text(), 300, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
