import spark {
    Spark,
    Request,
    Response,
    Route
}
import java.lang {
    Thread,
    Runnable
}

"Run the module `com.athaydes.sparkweb`."
shared void run() {
    object hello satisfies Route { 
        shared actual Object handle (Request request, Response response) {
            print("Received request accepting ``request.headers("Accept") else "null"``");
            return "Hello Ceylon!";
        }
    }
    
    object bye satisfies Route { 
        object stop satisfies Runnable {
            shared actual void run() {
                Thread.sleep(250); // so the server responds to the request before dying!
                print("Stopping Server");
                Spark.stop();
            }
        }
        
        shared actual Object handle (Request request, Response response) {
            Thread(stop).start();
            return "Bye!";
        }
    }
    
    Spark.get("hello", hello);
    Spark.get("bye", bye);
}