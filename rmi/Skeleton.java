package rmi;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/** RMI skeleton

    <p>
    A skeleton encapsulates a multithreaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parametrized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
*/
public class Skeleton<T>
{
    T server;
    InetSocketAddress address;
    SkeletonThread<T> skeletonThread;
    Class<T> intf;
    ServerSocket socketServer;
    /** Creates a <code>Skeleton</code> with no initial server address. The
        address will be determined by the system when <code>start</code> is
        called. Equivalent to using <code>Skeleton(null)</code>.

        <p>
        This constructor is for skeletons that will not be used for
        bootstrapping RMI - those that therefore do not require a well-known
        port.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server)
    {
        // Ensure the interface is not null.
        if(c == null || server == null) {
            throw new NullPointerException();
        }

        // Ensure the input c is an interface.
        if(!c.isInterface()) {
            throw new Error();
        }

        // Ensure the interface throw RMIException which make sure it an remote interface
        if(!isRemoteInterface(c)) {
            throw new Error();
        }

        // Ensure the server object has implemented the interface c.
        if(!isAssignableFromServer(c, server.getClass())) {
            throw new Error();
        }
        this.server = server;
        this.intf = c;
    }

    /** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address)
    {
        this(c, server);
        this.address = address;
    }

    /** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause)
    {
        if (cause != null) {
            cause.printStackTrace();
        }
    }

    /** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception)
    {
        stop();
        return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
        if (!exception.getClass().equals(EOFException.class)) {
            exception.printStackTrace();
        }
    }

    /** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socket cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
     */
    public synchronized void start() throws RMIException
    {
        // When the server has already been started and has not since stopped.
        if(this.socketServer != null && !this.socketServer.isClosed()) {
            throw new RMIException("The server has already been started and has not since stopped");
        }

        try{
            if(this.address == null) {
                this.socketServer = new ServerSocket(0);
                //System.out.printf("\n\n----- Start a Skeleton on default port %s-----\n", this.socketServer.getInetAddress());
                this.address = new InetSocketAddress(this.socketServer.getInetAddress(),
                                                        this.socketServer.getLocalPort());
            }
            else {
                //System.out.printf("\n\n----- Start a Skeleton on %d-----\n", this.address.getPort());
                this.socketServer = new ServerSocket(
                                                this.address.getPort(),
                                                1000,
                                                this.address.getAddress()
                                                );
            }
        }
        catch(Exception e) {
            // service_error(new RMIException(e));
            // When the listening socket cannot be created or bound.
            throw new RMIException(e);
        }
        try {
        //System.out.println("\n\n-----Start Skeleton Thread-----");
            this.skeletonThread = (new SkeletonThread<T>(this, this.socketServer, this.address,
                                                    this.intf, this.server));
        //System.out.printf("\n\n----- Waiting for a connection on %s:%d-----\n",
        //                        this.address.getHostName(), this.address.getPort());
            this.skeletonThread.start();
        }
        catch (Exception e){

            // When the listening thread cannot be created
            listen_error(e);

            throw new RMIException(e);
        }
    }

    /** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
     */
    public synchronized void stop()
    {
        // this.address = null;
        try {
            if(this.socketServer != null && !this.socketServer.isClosed())
                socketServer.close();
        }
        catch (Exception e) {
            listen_error(e);
        }
        try {
            skeletonThread.join();
            stopped(null);
        }
        catch(Exception e) {
            e.printStackTrace();
            listen_error(e);
        }
    }

    ////////////////////////////////////// Helper Function /////////////////////////////////////////
    private boolean isAssignableFromServer(Class<T> intf, Class<?> server) {
        return intf.isAssignableFrom(server);
    }

    private boolean isRemoteInterface(Class<T> intf) {
        Method[] methods = intf.getMethods();
        for (Method m: methods) {
            Class<?>[] ecpt = m.getExceptionTypes();
            boolean found = false;
            for(Class<?> c: ecpt) {
                if(c.getName().equals("rmi.RMIException")) {
                    found = true;
                    break;
                }
            }
            if(!found)
                return false;
        }
        return true;
    }
}
