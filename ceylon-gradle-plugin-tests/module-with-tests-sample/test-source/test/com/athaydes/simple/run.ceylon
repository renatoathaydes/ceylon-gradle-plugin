import ceylon.test {
    ...
}
import com.athaydes.simple { add }

test
shared void ceylonTest() {
    assertEquals(add(2, 2), 4);
}

test
shared void anotherTest() {
    assertEquals(add(1, 0), 1);
}
