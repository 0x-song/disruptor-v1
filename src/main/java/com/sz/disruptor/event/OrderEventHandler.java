package com.sz.disruptor.event;

import com.sz.disruptor.model.OrderEventModel;

/**
 * @Author
 * @Date 2024-12-08 15:32
 * @Version 1.0
 * 订单事件处理器
 */
public class OrderEventHandler implements EventHandler<OrderEventModel>{
    @Override
    public void consume(OrderEventModel event, long sequence, boolean endOfBatch) {
        System.out.println("消费者消费事件" + event + " sequence=" + sequence + " endOfBatch=" + endOfBatch);
    }
}
