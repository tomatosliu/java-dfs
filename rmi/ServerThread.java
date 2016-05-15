package rmi;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;

public class ServerThread<T> extends Thread {
    Socket socket = null;
    T server = null;
    Class<T> intf;
    Skeleton<T> skeleton;

    public ServerThread(Skeleton<T> skeleton, Socket socket, T server, Class<T> intf) {
        this.skeleton = skeleton;
        this.socket = socket;
        this.server = server;
        this.intf = intf;
    }

    public void run() {
        ObjectOutputStream objOutput = null;
        ObjectInputStream objInput = null;
        Object ret = null;

        try {
            objOutput = new ObjectOutputStream(this.socket.getOutputStream());
            objOutput.flush();
            objInput = new ObjectInputStream(this.socket.getInputStream());
        }
        catch(Exception e) {
            System.err.println(e.getMessage());
            close(objInput);
            close(objOutput);
            // Exception thrown in service response.
            this.skeleton.service_error(new RMIException("Exception thrown in service response."));
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            String methodName = (String) objInput.readObject();
            @SuppressWarnings("unchecked")
            Class<T>[] paramTypes = (Class<T>[]) objInput.readObject();
            @SuppressWarnings("unchecked")
            Object[] params = (Object[]) objInput.readObject();

            this.intf.getMethod(methodName, paramTypes);

            Method method = this.server.getClass().getMethod(methodName, paramTypes);


            if(!method.isAccessible()) {
                method.setAccessible(true);
            }
            System.out.println("+++++++++++++++++++ Invoked: " + method.getName());

            ret = method.invoke(this.server, params);
        }
        catch(Exception e) {
            if(e instanceof InvocationTargetException) {
                System.out.println("++++++++++++++++++ Invoke Exception: " + e.getCause());
                ret = e;
            }
            else {
                //throw new RMIException(e);
                close(objInput);
                close(objOutput);
                // Exception thrown in service response.
                this.skeleton.service_error(new RMIException("Exception thrown in service response.", e));
                return;
            }
        }

        try {
            objOutput.writeObject(ret);
        }
        catch(Exception e) {
            // Exception thrown in service response.
            this.skeleton.service_error(new RMIException("Exception thrown in service response."));
        }
        finally {
            close(objInput);
            close(objOutput);
        }
    }

    public static void close(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException e) {
            // ignore the exception
        }
    }
}
