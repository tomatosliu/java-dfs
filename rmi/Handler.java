package rmi;

import java.io.*;
import java.net.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.Exception.*;

/**
 * @author: Yilun Zhang, Qiao Zhang
 * @date: 2016/04/26
 */

/** RMI handler

    <p>
    A handler is a sub thread created by monitor to truly handle the task
    on the server side. Its creation is triggered by the message received
    by the skeleton/monitor.

*/

public class Handler extends Thread {

	/** Attributes maintained by handler.
     */
	private Socket listener = null;
	private Skeleton skeleton = null;

	/** Creates a <code>Handler</code> with associated socket and skeleton.
     */
	public Handler (Socket l, Skeleton skltn) {
		this.listener = l;
		this.skeleton = skltn;
	}

	/** Run the sub-thread to invoke given method of the remote object.
	 */
	public void run() {
		try {

			ObjectOutputStream out = new ObjectOutputStream(listener.getOutputStream());
    		out.flush();
    		ObjectInputStream in = new ObjectInputStream(listener.getInputStream());

    		RMImessage requestMsg = (RMImessage)in.readObject();
    		RMImessage responseMsg = null;
    		Object proxy = requestMsg.getProxy();
    		String methodName = requestMsg.getMethodName();
    		Object types = requestMsg.getTypes();
    		Object[] args = requestMsg.getArgs();

            Method method = skeleton.getRemoteInterface().getMethod((String)methodName,(Class[])types);

            Throwable to = null;

            try {
	    	    Object returnVal = method.invoke(skeleton.getRemoteObject(), args);
		    		responseMsg = new RMImessage(returnVal);
		    } catch (Throwable e) {
		    	responseMsg = new RMImessage(e.getCause(), true);
		    }

    		out.writeObject(responseMsg);
    		listener.close();

		} catch (Throwable e) {
			skeleton.service_error(new RMIException(e.getCause()));
		}
	}
}
