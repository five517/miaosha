package com.mario.secondkill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mario.secondkill.entity.Goods;
import com.mario.secondkill.vo.GoodsVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author hexiangdong
 * @since 2022-03-07
 */
public interface IGoodsService extends IService<Goods> {

    List<GoodsVo> getGoodsVo();

    GoodsVo getGoodsVoById(Long goodsId);
}
