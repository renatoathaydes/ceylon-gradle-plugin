import org.apache.logging.log4j {
	Logger,
	LogManager
}
import java.lang {
	Thread
}

shared class MyTest() {}

Logger logger = LogManager.getLogger(`class MyTest`);

"Run the module `com.athaydes.maven`."
shared void run() {
	logger.info("Hello Maven!");
	Thread.sleep(1000);
}