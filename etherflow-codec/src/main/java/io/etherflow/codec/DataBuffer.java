package io.etherflow.codec;

import java.io.InputStream;
import java.nio.ByteBuffer;

public interface DataBuffer {

    byte[] asByteArray();

    ByteBuffer asByteBuffer();

    InputStream asInputStream();

    int readableByteCount();

    void write(byte[] bytes);

    void write(byte b);

    void release();
}
