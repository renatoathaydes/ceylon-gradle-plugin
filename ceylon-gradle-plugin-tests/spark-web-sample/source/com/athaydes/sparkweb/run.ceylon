import spark {
    Spark
}
import java.lang {
    Thread,
    Runnable
}

object stop satisfies Runnable {
    shared actual void run() {
        Thread.sleep(250); // so the server responds to the request before dying!
        print("Stopping Server");
        Spark.stop();
    }
}

"Run the module `com.athaydes.sparkweb`."
shared void run() {
    Spark.get("hello", (request, response) {
        print("Received request accepting ``request.headers("Accept") else "null"``");
        return "Hello Ceylon!";
    });
    Spark.get("bye", (request, response) {
        Thread(stop).start();
        return "Bye!";
    });
}