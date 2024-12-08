package com.sz.disruptor;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.buffer.RingBuffer;
import com.sz.disruptor.event.OrderEventHandler;
import com.sz.disruptor.model.OrderEventModel;
import com.sz.disruptor.model.factory.OrderEventModelFactory;
import com.sz.disruptor.processor.BatchEventProcessor;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.BlockingWaitStrategy;

public class DisruptorV1Application {

    public static void main(String[] args) throws InterruptedException {
        int ringBufferSize = 16;
        RingBuffer<OrderEventModel> ringBuffer = RingBuffer.createSingleProducer(new OrderEventModelFactory(), ringBufferSize, new BlockingWaitStrategy());
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        //本质是runnable，供消费者线程调用
        BatchEventProcessor<OrderEventModel> eventProcessor = new BatchEventProcessor<>(ringBuffer, new OrderEventHandler(), sequenceBarrier);

        Sequence currentConsumerSequence = eventProcessor.getCurrentConsumeSequence();

        ringBuffer.setConsumerSequence(currentConsumerSequence);

        //启动消费者线程
        new Thread(eventProcessor).start();

        Thread.sleep(5000);

        for (int i = 0; i < 100; i++) {
            long nextIndex = ringBuffer.next();
            OrderEventModel orderEventModel = ringBuffer.get(nextIndex);
            orderEventModel.setMessage("message" + i);
            orderEventModel.setPrice(i * 10);
            System.out.println("生产者发布事件:" + orderEventModel);
            ringBuffer.publish(nextIndex);
            if(i == 70){
                Thread.sleep(2000);
            }
        }
    }

}
