import org.springframework.boot { ... }
import org.springframework.boot.autoconfigure { ... }
import org.springframework.stereotype { ... }
import org.springframework.web.bind.annotation { ... }

import java.lang { JString=String }
import ceylon.interop.java { javaClass, javaString }

controller
enableAutoConfiguration
shared class SampleController() {

    requestMapping({ "/" })
    responseBody
    shared JString home() {
        return javaString("Hello World!");
    }

}

"Run the module `com.athaydes.springboot`."
shared void run() {
    SpringApplication.run(javaClass<SampleController>());
}
