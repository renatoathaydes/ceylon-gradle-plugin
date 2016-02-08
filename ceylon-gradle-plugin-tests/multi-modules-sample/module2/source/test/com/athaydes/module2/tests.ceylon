import ceylon.test { ... }

import com.athaydes.module2 { getPaddedMessage }

shared test
void getPaddedMessageTest() {
    assertEquals(getPaddedMessage(" Hello!", 10, '*'), "*** Hello!");
    assertEquals(getPaddedMessage("", 10, '*'), "**********");
    assertEquals(getPaddedMessage("Hej", 2, '*'), "Hej");
    assertEquals(getPaddedMessage("Hej", 0, '*'), "Hej");
    assertEquals(getPaddedMessage("Hej", -10, '*'), "Hej");
}
