package io.etherflow.codec.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.etherflow.codec.*;
import io.etherflow.core.Mono;

public class JacksonCodec implements HttpMessageReader<Object>, HttpMessageWriter<Object> {

    private final ObjectMapper objectMapper;

    public JacksonCodec() {
        this(createDefaultMapper());
    }

    public JacksonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private static ObjectMapper createDefaultMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.INDENT_OUTPUT, false)
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .build();
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    @Override
    public Mono<Object> read(Class<?> type, DataBuffer buffer, MediaType mediaType) {
        return Mono.fromCallable(() -> objectMapper.readValue(buffer.asInputStream(), type));
    }

    @Override
    public boolean canRead(Class<?> type, MediaType mediaType) {
        return mediaType != null && mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
    }

    @Override
    public Mono<DataBuffer> write(Object value, MediaType mediaType) {
        return Mono.fromCallable(() -> {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            DataBufferFactory factory = new DefaultDataBufferFactory();
            DataBuffer buf = factory.allocateBuffer(bytes.length);
            buf.write(bytes);
            return buf;
        });
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return mediaType != null && mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
    }

    public <T> Mono<T> readValue(DataBuffer buffer, Class<T> type) {
        return Mono.fromCallable(() -> objectMapper.readValue(buffer.asInputStream(), type));
    }

    public <T> Mono<DataBuffer> writeValue(T value) {
        return write(value, MediaType.APPLICATION_JSON);
    }
}
