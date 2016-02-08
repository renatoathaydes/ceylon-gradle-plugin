import com.athaydes.module1 { ... }
import com.athaydes.ceylon_gradle_plugin.java_module { MyJavaClass }

shared void run() {
    print("Module2 running...");
    print("Hi, what's your name, other module? ``module1.name``");

    value javaClass = MyJavaClass("Running in Java");
    value paddedMessage = javaClass.getPaddedMessage(50, '_');

    print("Hey, Java class, give me a padded message! ``paddedMessage``");
}

shared String getPaddedMessage(String message, Integer characterCount, Character paddingCharacter)
    => MyJavaClass(message).getPaddedMessage(characterCount, paddingCharacter);
