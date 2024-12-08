package com.sz.disruptor.sequence;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Author
 * @Date 2024-12-07 20:17
 * @Version 1.0
 * 序列号对象，需要被生产者、消费者所共享，内部使用了一个volatile修饰的变量
 * 基于阻塞/非阻塞模型的悲观锁和基于CAS的乐观锁在高并发情况下都有一定的性能损耗
 * disruptor使用的是仅依靠内存屏障提供的多线程间内存可见性能力
 */
public class Sequence {

    private volatile long value = -1;

    private static final Unsafe unsafe;

    private static final long value_offset;


    static {

        Field getUnsafe = null;
        try {
            getUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            getUnsafe.setAccessible(true);
            unsafe = (Unsafe) getUnsafe.get(null);
            //获取字段在内存中的偏移量
            value_offset = unsafe.objectFieldOffset(Sequence.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public Sequence() {
    }

    public Sequence(long value) {
        this.value = value;
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        this.value = value;
    }

    // unsafe.putOrderedLong‌是Unsafe类中的一个方法，用于以有序的方式写入一个long类型的值到指定的内存地址。
    // 这个方法与volatile关键字不同，它不会强制立即对其他线程可见，
    // 而是通过一种更快的存储-存储屏障来实现写入操作，从而减少线程间的同步开销
    public void lazySet(long value) {
        unsafe.putOrderedLong(this, value_offset, value);
    }
}
