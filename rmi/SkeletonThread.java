package rmi;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class SkeletonThread<T> extends Thread {
    ArrayList<ServerThread<T>> threads = new ArrayList<ServerThread<T>>();
    InetSocketAddress address;
    Class<T> intf;
    T server;
    ServerSocket socketServer;
    Skeleton<T> skeleton;

    public SkeletonThread(Skeleton<T> skeleton, ServerSocket socketServer, InetSocketAddress address,
                Class<T> intf, T server) {
        this.skeleton = skeleton;
        this.socketServer = socketServer;
        this.address = address;
        this.intf = intf;
        this.server = server;
    }

    public void run() {
        try {
            while(true) {
                //System.out.println("-----Waiting for a connection...-----");
                Socket socket = this.socketServer.accept();
                //System.out.println("Client Connected!");
                ServerThread<T> thread = (new ServerThread<T>(this.skeleton, socket, this.server, this.intf));
                thread.start();
                threads.add(thread);
            }
        }
        catch(SocketException e) {
            try {
                if(this.socketServer.isClosed())
                    for(ServerThread<T> t: this.threads) {
                        t.join();
                    }
            }
            catch(InterruptedException ep) {

            }
        }
        catch(IOException e) {
            this.skeleton.listen_error(e);
        }
    }
}
