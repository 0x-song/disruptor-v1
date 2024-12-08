package com.sz.disruptor.buffer;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.model.factory.EventModelFactory;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.sequence.SingleProducerSequencer;
import com.sz.disruptor.strategy.WaitStrategy;

/**
 * @Author
 * @Date 2024-12-08 11:17
 * @Version 1.0
 * 生产者、消费者通过序列号进行通信，但最终事件消息存放的载体依然是RingBuffer环形队列。
 * v1版本的环形队列由三大核心组件组成：对象数组（elementList）、生产者序列器（singleProducerSequencer）、事件工厂（eventModelFactory）。
 * 初始化时，构造函数中通过eventModelFactory预先将整个队列填满事件对象，后续生产发布时只更新属性，不新增、删减队列中的事件对象。
 * 序列号对队列长度ringBufferSize-1求余，可以获得序列号在对象数组中的实际下标（比如队列长度8，序列号25，则序列号25对应的实际下标为25%8=1）。
 * 由于计算机二进制存储的特性，对2的幂次方长度-1进行求余可以优化为位运算。
 * 例如序列号25的二进制值为11001，对7求余可以转换为对00111进行且运算得到后三位001（1），
 * 对15求余可以转换为对01111进行且运算得到后4位1001（9），在CPU硬件上作位运算会比普通的除法运算更快
 * （这也是jdk HashMap中容量设置为2次幂的一个重要原因）。
 */
public class RingBuffer<T> {

    private final T[] elementList;

    private final SingleProducerSequencer singleProducerSequencer;

    private final int ringBufferSize;

    private final int mask;

    public RingBuffer(SingleProducerSequencer singleProducerSequencer, EventModelFactory<T> eventModelFactory){
        int bufferSize = singleProducerSequencer.getRingBufferSize();
        if(Integer.bitCount(bufferSize) != 1){
            //必须是2的幂
            throw new IllegalArgumentException("Buffer size must be a power of 2");
        }

        this.singleProducerSequencer = singleProducerSequencer;

        this.ringBufferSize = bufferSize;

        this.elementList = (T[]) new Object[ringBufferSize];
        //回环掩码
        this.mask = ringBufferSize - 1;

        //填充事件模型对象，后续更改状态
        for (int i = 0; i < elementList.length; i++) {
            this.elementList[i] = eventModelFactory.newInstance();
        }
    }

    public T get(long sequence) {
        // 由于ringBuffer的长度是2次幂，mask为2次幂-1，因此可以将求余运算优化为位运算
        int index = (int) (sequence & mask);
        return elementList[index];
    }

    public long next(){
        return this.singleProducerSequencer.next();
    }

    public long next(int n){
        return this.singleProducerSequencer.next(n);
    }

    public void publish(Long index){
        this.singleProducerSequencer.publish(index);
    }

    public void setConsumerSequence(Sequence consumerSequence){
        this.singleProducerSequencer.setConsumerSequence(consumerSequence);
    }

    public SequenceBarrier newBarrier() {
        return this.singleProducerSequencer.newBarrier();
    }

    public static <E> RingBuffer<E> createSingleProducer(EventModelFactory<E> factory, int bufferSize, WaitStrategy waitStrategy) {
        SingleProducerSequencer sequencer = new SingleProducerSequencer(bufferSize, waitStrategy);
        return new RingBuffer<>(sequencer,factory);
    }
}
