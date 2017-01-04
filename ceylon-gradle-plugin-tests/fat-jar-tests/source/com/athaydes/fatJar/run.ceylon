import org.apache.logging.log4j {
	Logger,
	LogManager
}
import ceylon.interop.java { javaClass }

shared class MyTestClass() {}

Logger logger = LogManager.getLogger(javaClass<MyTestClass>());

"Run the module `com.athaydes.fatJar`."
shared void run() {
	logger.info("Hello from Ceylon fatJAR!");
}
