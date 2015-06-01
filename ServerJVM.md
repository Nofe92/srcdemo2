# Client/Server JVMs #

The Java "HotSpot" JVM comes into two flavors: [a "client" and a "server" version](http://docs.oracle.com/javase/1.3/docs/guide/performance/hotspot.html). [The difference between the two](http://stackoverflow.com/questions/198577/real-differences-between-java-server-and-java-client) comes down to this:
  * The `client` JVM starts faster
  * The `server` JVM runs faster (in the long run)
Since SrcDemo² is not the kind of application that you close and open every 15 seconds, the `server` JVM is more appropriate for its kind of workload. Additionally, the `server` JVM comes with additional optimizations available as command-line arguments, which SrcDemo² will detect and use.

# How to get a `server` JVM #

## Windows ##
  * Go to the [Java SE downloads page](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
  * Scroll down to the "Java SE 6" section
  * Download the **32-bit JDK** (not the JRE)
  * Install it and open SrcDemo² to see if everything worked
  * If it did not work:
    * Open the installation directory. There should be a `bin` folder in there, in which you should find another folder called `server`.
    * Open the regular Java installation directory (the JRE). Go to its `bin` folder.
    * Copy the `server` folder from the JDK's `bin` folder to the JRE's `bin` folder.