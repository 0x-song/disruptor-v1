package com.sz.disruptor.sequence;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.strategy.WaitStrategy;

import java.util.concurrent.locks.LockSupport;

/**
 * @Author
 * @Date 2024-12-07 20:51
 * @Version 1.0
 * 单线程生成者序列器
 * 因为是单线程序列器，所以在设计上面就不是线程安全的
 */
public class SingleProducerSequencer {

    private int ringBufferSize;

    //当前已发布的生产者序列号
    private final Sequence currentProducerSequence = new Sequence();

    //消费者序列
    private Sequence consumerSequence;

    private final WaitStrategy waitStrategy;

    //当前已经申请的序号，单线程生产者内部使用
    private long nextValue = -1;

    //当前已经缓存的消费者序列；单线程生产者内部使用，就是普通的long类型
    private long cachedConsumerSequence = -1;

    public SingleProducerSequencer(int ringBufferSize, WaitStrategy waitStrategy) {
        this.ringBufferSize = ringBufferSize;
        this.waitStrategy = waitStrategy;
    }

    public long next(){
        return next(1);
    }

    /**
     *
     * @param n 一次申请可用的n个生产者序列号
     * @return
     */
    public long next(int n){
        //申请的下一个生产者节点
        long nextProducerSequence = nextValue + n;
        //需要考虑到生产者超过消费者一圈的情形
        long wrapPoint = nextProducerSequence - ringBufferSize;

        //消费者序号并没有实时获取
        long cachedGatingSequence = this.cachedConsumerSequence;

        if(wrapPoint > cachedGatingSequence){

            long minSequence;
            while (wrapPoint > (minSequence = consumerSequence.get())){
                //如果生产者超过了一圈，则进行间歇性park阻塞
                LockSupport.parkNanos(1L);
            }

            //不是实时获取，可能再次得到的cachedValue要比上一次大很多
            //比如生产者现在是5，消费者是2，根据上述逻辑不会超，则很长一段时间都不会走到if语句中
            //不会更新cached consumer序号，直到再次wrapPoint再次来到了2之后，满足了if，更新
            this.cachedConsumerSequence = minSequence;
        }
        this.nextValue = nextProducerSequence;
        return nextProducerSequence;
    }

    public void publish(long publishIndex){
        // 发布时，更新生产者队列
        // lazySet，由于消费者可以批量的拉取数据，所以不必每次发布时都volatile的更新，允许消费者晚一点感知到，这样性能会更好
        // 设置写屏障
        this.currentProducerSequence.lazySet(publishIndex);

        // 发布完成后，唤醒可能阻塞等待的消费者线程
        this.waitStrategy.signalWhenBlocking();
    }

    public SequenceBarrier newBarrier(){
        return new SequenceBarrier(this.currentProducerSequence,this.waitStrategy);
    }

    public void setConsumerSequence(Sequence consumerSequence){
        this.consumerSequence = consumerSequence;
    }

    public int getRingBufferSize() {
        return ringBufferSize;
    }

}
