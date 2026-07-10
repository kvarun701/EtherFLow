package io.etherflow.codec;

public interface DataBufferFactory {

    DataBuffer allocateBuffer(int initialCapacity);

    DataBuffer wrap(byte[] bytes);
}
