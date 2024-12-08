package com.sz.disruptor.processor;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.buffer.RingBuffer;
import com.sz.disruptor.event.EventHandler;
import com.sz.disruptor.sequence.Sequence;

/**
 * @Author
 * @Date 2024-12-08 11:03
 * @Version 1.0
 * 单线程消费者
 * 通过一个循环不断监听生产者的生产进度，批量获取已经发布的可以访问的、消费的事件对象
 * 消费者通过SequenceBarrier感知生产者的生产进度，控制自己的消费序列不超过生产者
 * 消费逻辑由EventHandler接口的处理器控制
 */
public class BatchEventProcessor<T> implements Runnable{

    private final Sequence currentConsumerSequence = new Sequence(-1);

    private final RingBuffer<T> ringBuffer;

    private final EventHandler<T> eventConsumer;

    private final SequenceBarrier sequenceBarrier;

    public BatchEventProcessor(RingBuffer<T> ringBuffer,
                               EventHandler<T> eventConsumer,
                               SequenceBarrier sequenceBarrier) {
        this.ringBuffer = ringBuffer;
        this.eventConsumer = eventConsumer;
        this.sequenceBarrier = sequenceBarrier;
    }

    @Override
    public void run() {
        long nextConsumerIndex = currentConsumerSequence.get() + 1;

        while (true){
            long availableConsumerIndex = sequenceBarrier.getAvailableConsumerSequence(nextConsumerIndex);
            while (nextConsumerIndex <= availableConsumerIndex){
                //可以向后继续消费 比如当前在4号，但是生产者在5号，那么available=4，next=5，不满足条件
                T event = ringBuffer.get(nextConsumerIndex);
                eventConsumer.consume(event, nextConsumerIndex, nextConsumerIndex == availableConsumerIndex);
                nextConsumerIndex++;
            }
            currentConsumerSequence.lazySet(availableConsumerIndex);
            System.out.println("更新当前消费者的消费序列:" + availableConsumerIndex);

        }
    }

    public Sequence getCurrentConsumeSequence() {
        return this.currentConsumerSequence;
    }
}
