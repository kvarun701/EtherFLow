package io.etherflow.codec;

import io.etherflow.codec.json.JacksonCodec;
import org.junit.jupiter.api.Test;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class JacksonCodecTest {

    static class User {
        public String name;
        public int age;

        public User() {}

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof User u)) return false;
            return age == u.age && Objects.equals(name, u.name);
        }

        @Override
        public int hashCode() { return Objects.hash(name, age); }
    }

    @Test
    void canReadJson() {
        JacksonCodec codec = new JacksonCodec();
        assertTrue(codec.canRead(User.class, MediaType.APPLICATION_JSON));
        assertFalse(codec.canRead(User.class, MediaType.TEXT_PLAIN));
    }

    @Test
    void canWriteJson() {
        JacksonCodec codec = new JacksonCodec();
        assertTrue(codec.canWrite(User.class, MediaType.APPLICATION_JSON));
        assertFalse(codec.canWrite(User.class, MediaType.TEXT_HTML));
    }

    @Test
    void readAndWrite() {
        JacksonCodec codec = new JacksonCodec();
        User user = new User("Alice", 30);

        DataBuffer buf = codec.write(user, MediaType.APPLICATION_JSON).block();
        assertNotNull(buf);

        User result = codec.readValue(buf, User.class).block();
        assertEquals(user, result);
    }

    @Test
    void readInvalidJson() {
        JacksonCodec codec = new JacksonCodec();
        DataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer buf = factory.wrap("not json".getBytes());

        assertThrows(Exception.class, () ->
                codec.read(User.class, buf, MediaType.APPLICATION_JSON).block());
    }
}
