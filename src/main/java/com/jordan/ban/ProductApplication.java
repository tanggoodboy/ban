package com.jordan.ban;

import com.jordan.ban.domain.*;
import com.jordan.ban.market.parser.*;
import com.jordan.ban.market.policy.MarketDiffer;
import com.jordan.ban.market.trade.TradeHelper;
import com.jordan.ban.mq.MessageSender;
import com.jordan.ban.mq.spring.Sender;
import com.jordan.ban.utils.JSONUtil;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class ProductApplication {


    @Autowired
    private Sender sender;

    private void getDepth(String symbol, String marketName1, String marketName2, long period) {
        String depthTopic = symbol + "-depth";
        // 分析买卖盘
        MarketParser m1 = MarketFactory.getMarket(marketName1);
        MarketParser m2 = MarketFactory.getMarket(marketName2);
        Timer timer1 = new Timer();
        timer1.schedule(new TimerTask() {
            @Override
            public void run() {
                Map<String, Object> mockTrade = new HashMap<>();
                // FIXME: use asynchronous
                long start = System.currentTimeMillis();
                Depth depth1 = m1.getDepth(symbol);
                Depth depth2 = m2.getDepth(symbol);
                double d1ask = depth1.getAsks().get(0).getPrice();
                double d1askVolume = depth1.getAsks().get(0).getVolume();
                double d1bid = depth1.getBids().get(0).getPrice();
                double d1bidVolume = depth1.getBids().get(0).getVolume();
                double d2ask = depth2.getAsks().get(0).getPrice();
                double d2askVolume = depth2.getAsks().get(0).getVolume();
                double d2bid = depth2.getBids().get(0).getPrice();
                double d2bidVolume = depth2.getBids().get(0).getVolume();
                MarketDepth marketDepth = new MarketDepth(d1ask, d1askVolume, d1bid, d1bidVolume, d2ask, d2askVolume, d2bid, d2bidVolume);
                mockTrade.put("a2b", a2b(marketDepth, depth1, depth2, (System.currentTimeMillis() - start), System.currentTimeMillis()));
                mockTrade.put("b2a", b2a(marketDepth, depth1, depth2, (System.currentTimeMillis() - start), System.currentTimeMillis()));
                sender.send(depthTopic, JSONUtil.toJsonString(mockTrade));

            }
        }, 0, period);
    }

    private MockTradeResultIndex a2b(MarketDepth marketDepth, Depth depth1, Depth depth2, long costTime, long createTime) {
        MockTradeResult eatAB = TradeHelper.eatA2B(marketDepth);
        MockTradeResult tradeAB = TradeHelper.tradeA2B(marketDepth);
        MockTradeResultIndex indexAB = new MockTradeResultIndex();
        indexAB.setEatDiff(eatAB.getTradeDiff());
        indexAB.setEatPercent(eatAB.getTradePercent());
        indexAB.setTradeDiff(tradeAB.getTradeDiff());
        indexAB.setTradePercent(tradeAB.getTradePercent());
        indexAB.setTradeDirect(eatAB.getTradeDirect());
        indexAB.setCostTime(costTime);
        indexAB.setCreateTime(new Date(createTime));
        indexAB.setSymbol(depth1.getSymbol().toUpperCase());
        indexAB.setDiffPlatform(depth1.getPlatform() + "-" + depth2.getPlatform());
        indexAB.setTradeVolume(tradeAB.getMinTradeVolume());
        indexAB.setEatTradeVolume(eatAB.getMinTradeVolume());
        indexAB.setSellCost(eatAB.getSellCost());
        indexAB.setBuyCost(eatAB.getBuyCost());
        return indexAB;
    }

    private MockTradeResultIndex b2a(MarketDepth marketDepth, Depth depth1, Depth depth2, long costTime, long createTime) {
        MockTradeResult eatBA = TradeHelper.eatB2A(marketDepth);
        MockTradeResult tradeBA = TradeHelper.tradeB2A(marketDepth);
        MockTradeResultIndex indexBA = new MockTradeResultIndex();
        indexBA.setEatDiff(eatBA.getTradeDiff());
        indexBA.setEatPercent(eatBA.getTradePercent());
        indexBA.setTradeDiff(tradeBA.getTradeDiff());
        indexBA.setTradePercent(tradeBA.getTradePercent());
        indexBA.setTradeDirect(tradeBA.getTradeDirect());
        indexBA.setCostTime(costTime);
        indexBA.setCreateTime(new Date(createTime));
        indexBA.setSymbol(depth1.getSymbol().toUpperCase());
        indexBA.setDiffPlatform(depth1.getPlatform() + "-" + depth2.getPlatform());
        indexBA.setTradeVolume(tradeBA.getMinTradeVolume());
        indexBA.setEatTradeVolume(eatBA.getMinTradeVolume());
        indexBA.setSellCost(eatBA.getSellCost());
        indexBA.setBuyCost(eatBA.getBuyCost());
        return indexBA;
    }

    public void diffTask(String symbol, String market1, String market2, long period) throws ExecutionException, InterruptedException {
//        diffMarket(symbol, market1, market2, period);
        getDepth(symbol, market1, market2, period);
    }
}
