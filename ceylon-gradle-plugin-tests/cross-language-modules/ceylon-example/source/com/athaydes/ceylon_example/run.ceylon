import com.athaydes.groovy { MyGroovyClass }
import com.athaydes.java { JavaExample }
import com.athaydes.kotlin { KotlinExample }

shared void run() {
    // use Groovy
    value separator = MyGroovyClass.groovyMultiply("=", 50);
    print(separator);

    print("This is the ceylon_example module running!");

    // use Java
    value urlEncoded = JavaExample.replace("This is a text message", " ", "%20");
    print("URL encoded message: ``urlEncoded``");

    // use Kotlin
    value greeting = KotlinExample().sayHi("Ceylon");
    print(greeting);

    print(separator);
}
