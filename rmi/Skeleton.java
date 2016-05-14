package rmi;

import java.net.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.Exception.*;

/**
 * @author: Yilun Zhang, Qiao Zhang
 * @date: 2016/04/26
 */

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

    /** Attributes maintained by skeleton.
     */
    private Class remoteInterface = null;
    private Object remoteObject = null;
    private int portNumber = 0;
    private String hostName = null;
    private volatile boolean running = false;
    private ServerSocket serverSocket = null;
    private Monitor monitor = null;

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
        if(c == null || server == null)
            throw new NullPointerException();

        if(!c.isInterface() || !errorCheck(c))
            throw new Error("Invalid remote interface!");

        this.remoteInterface = c;
        this.remoteObject = server;
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
        if(c == null || server == null)
            throw new NullPointerException();

        if(!c.isInterface() || !errorCheck(c))
            throw new Error("Invalid remote interface!");

        this.remoteInterface = c;
        this.remoteObject = server;

        if(address != null){
           this.portNumber = address.getPort();
           this.hostName = address.getHostName();
        }
        
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
        if(running) 
            return true;
        else 
            return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
        System.out.println(exception.getCause());
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
        if(!running) {
            running = true;
            try {
                serverSocket = new ServerSocket(portNumber);
                if(portNumber == 0)
                    this.portNumber = serverSocket.getLocalPort();
                if(this.hostName == null)
                    this.hostName = serverSocket.getInetAddress().getHostName();
                monitor = new Monitor(serverSocket, this);
                monitor.start();
            } catch (IOException e) {
                throw new RMIException(e.getCause());
            }
        }
        else{ 
            throw new RMIException("Skeleton already started.");
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
        if(running) {
            try {
                running = false;
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Failed to close the server socket!");
            }
        }                   
    }

    /** Return the running state of the skeleton.
     */
    public boolean getRunning() {
        return running;
    }

    /** Return the listening port number of the skeleton.
     */
    public int getPortNumber() {
        return portNumber;
    }

    /** Return the listening host name of the skeleton.
     */
    public String getHostName() {
        return hostName;
    }

    /** Return the listening remote interface of the skeleton.
     */
    public Object getRemoteObject() {
        return remoteObject;
    }

    /** Return the listening remote interface of the skeleton.
     */
    public Class getRemoteInterface() {
        return remoteInterface;
    }

    /** Check whether c is a valid remote interface.
     */
    private static <T> boolean errorCheck(Class<T> c){

        Method[] methods = c.getMethods();

        for(int i = 0; i<methods.length; i++){
            boolean contain = false;
            for(Class e: methods[i].getExceptionTypes()){
              if(e.getName().contains("RMIException")){
                contain = true;
                break;
              }
            }
            if(!contain) return false;
        }

        return true;

    }
}
