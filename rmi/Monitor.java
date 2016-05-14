package rmi;

import java.io.IOException;
import java.net.*;
import java.lang.Exception.*;

/**
 * @author: Yilun Zhang, Qiao Zhang
 * @date: 2016/04/26
 */

/** RMI monitor

    <p>
    A monitor is a main thread created by skeleton.start() to listen for the message 
    from stub. To handle concurrent messages, monitor creates sub-threads to process
    the real task. It is sub-thread's job to send the returned value back to the 
    client side.

*/

public class Monitor extends Thread {

	/** Attributes maintained by monitor.
     */
	private ServerSocket serverSocket = null;
	private Skeleton skeleton = null;
	private Exception cause = null;

	/** Creates a <code>Monitor</code> with associated serversocket and skeleton. 
     */
	public Monitor (ServerSocket ss, Skeleton skltn) {
		this.serverSocket = ss;
		this.skeleton = skltn;
	}

	public void run() {
		while(this.skeleton.getRunning() && !this.isInterrupted()) {
			try {
				Socket listener = serverSocket.accept();
				Handler handler = new Handler(listener, skeleton);
				handler.start();
			} catch (IOException e) {
				if(!this.skeleton.getRunning()) {
					this.interrupt();
					this.cause = e;
				}
			}
		}
		this.skeleton.stopped(cause);
	}
}