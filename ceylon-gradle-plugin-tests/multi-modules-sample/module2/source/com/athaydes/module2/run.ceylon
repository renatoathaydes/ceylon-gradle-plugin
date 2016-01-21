import com.athaydes.module1 { ... }

shared void run() {
    print("I depend on ``module1.name``");

    value javaClass = MyJavaClass("Running in Java");
    value paddedMessage = javaClass.getMessagePaddedLeft(50, '_');

    print("Ceylon code asking for a Java padded message: ``paddedMessage``");
}
