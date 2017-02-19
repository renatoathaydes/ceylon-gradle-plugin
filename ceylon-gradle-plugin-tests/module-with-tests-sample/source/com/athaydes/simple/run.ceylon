"Run the module `com.athaydes.simple`."
shared void run() {
    print("Hello World!");
    print("Args: ``process.arguments``");
}

shared void addArgs() {
    value args = process.arguments.collect(parseFloat);
    if (args.any((n) => n is Null)) {
        print("Invalid input. Only numbers are acceptable. Arguments: ``process.arguments``");
    } else {
        value numbers = args.coalesced;
        value total = numbers.fold(0.0)(plus<Float>);
        print("The sum of ``numbers`` is: ``total``");
    }
}

shared Integer add(Integer a, Integer b)
    => a + b;

shared class TopLevelRunnable() {

    print("Running TopLevelRunnable");

}