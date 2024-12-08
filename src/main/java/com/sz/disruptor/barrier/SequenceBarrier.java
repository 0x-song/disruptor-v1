package com.sz.disruptor.barrier;

import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.WaitStrategy;

/**
 * @Author
 * @Date 2024-12-07 22:20
 * @Version 1.0
 */
public class SequenceBarrier {

    private final Sequence currentProducerSequence;

    private final WaitStrategy waitStrategy;


    public SequenceBarrier(Sequence currentProducerSequence, WaitStrategy waitStrategy) {
        this.currentProducerSequence = currentProducerSequence;
        this.waitStrategy = waitStrategy;
    }

    public long getAvailableConsumerSequence(long currentConsumerIndex) {

        return waitStrategy.waitFor(currentConsumerIndex, currentProducerSequence, this);
    }
}
