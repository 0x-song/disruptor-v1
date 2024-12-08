package com.sz.disruptor.strategy;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.sequence.Sequence;

/**
 * @Author
 * @Date 2024-12-07 20:55
 * @Version 1.0
 */
public interface WaitStrategy {

    /**
     * 唤醒waitFor阻塞在该等待策略对象上的消费者线程
     */
    void signalWhenBlocking();

    /**
     * 如果不满足条件，那么便会阻塞在该方法
     * @param currentConsumerIndex
     * @param currentProducerSequence
     * @param sequenceBarrier
     * @return
     */
    long waitFor(long currentConsumerIndex, Sequence currentProducerSequence, SequenceBarrier sequenceBarrier);
}
