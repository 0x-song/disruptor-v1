package com.sz.disruptor.strategy;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.sequence.Sequence;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author
 * @Date 2024-12-08 12:19
 * @Version 1.0
 * 阻塞等待策略
 */
public class BlockingWaitStrategy implements WaitStrategy{

    private final Lock lock = new ReentrantLock();

    private final Condition processorNotifyCondition = lock.newCondition();


    @Override
    public void signalWhenBlocking() {
        lock.lock();
        try {
            processorNotifyCondition.signal();
        }catch (Exception e){

        }finally {
            lock.unlock();
        }
    }

    @Override
    public long waitFor(long currentConsumerIndex, Sequence currentProducerSequence, SequenceBarrier sequenceBarrier) {
        if(currentProducerSequence.get() < currentConsumerIndex){
            //生产者序号小于消费者序号,说明目前消费者速度大于生产者速度
            lock.lock();
            try {
                while (currentProducerSequence.get() < currentConsumerIndex){
                    processorNotifyCondition.await();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }

        }

        return currentConsumerIndex;
    }
}
