package io.etherflow.codec;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DefaultDataBufferFactory implements DataBufferFactory {

    @Override
    public DataBuffer allocateBuffer(int initialCapacity) {
        return new DefaultDataBuffer(new byte[initialCapacity], 0);
    }

    @Override
    public DataBuffer wrap(byte[] bytes) {
        return new DefaultDataBuffer(bytes, bytes.length);
    }

    static class DefaultDataBuffer implements DataBuffer {
        private byte[] buf;
        private int writePos;

        DefaultDataBuffer(byte[] buf, int writePos) {
            this.buf = buf;
            this.writePos = writePos;
        }

        @Override
        public byte[] asByteArray() {
            return Arrays.copyOf(buf, writePos);
        }

        @Override
        public ByteBuffer asByteBuffer() {
            return ByteBuffer.wrap(buf, 0, writePos);
        }

        @Override
        public InputStream asInputStream() {
            return new ByteArrayInputStream(buf, 0, writePos);
        }

        @Override
        public int readableByteCount() {
            return writePos;
        }

        @Override
        public void write(byte[] bytes) {
            ensureCapacity(bytes.length);
            System.arraycopy(bytes, 0, buf, writePos, bytes.length);
            writePos += bytes.length;
        }

        @Override
        public void write(byte b) {
            ensureCapacity(1);
            buf[writePos++] = b;
        }

        @Override
        public void release() {
            buf = new byte[0];
            writePos = 0;
        }

        private void ensureCapacity(int needed) {
            if (writePos + needed > buf.length) {
                int newSize = Math.max(buf.length * 2, writePos + needed);
                buf = Arrays.copyOf(buf, newSize);
            }
        }
    }
}
