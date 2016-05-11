package rmi;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;
import java.util.*;

public class RMIInvocationHandler implements InvocationHandler, Serializable {
    InetSocketAddress skeletonAddress;
    Class<?> intf;

    public RMIInvocationHandler(Class<?> c, InetSocketAddress address) {
        this.intf = c;
        this.skeletonAddress = address;
    }


    public boolean equalsStub(Object stub) {
        if(stub == null) {
            return false;
        }

        try {
            RMIInvocationHandler stubhandler = (RMIInvocationHandler) Proxy.getInvocationHandler(stub);
            if(this.skeletonAddress.toString().equals(stubhandler.skeletonAddress.toString())
                    && this.intf.toString().equals(stubhandler.intf.toString())) {
                return true;
            }
            else {
                return false;
            }
        }
        catch(Throwable e) {
            return false;
        }
    }

    public int hashCodeStub() {
        return (this.intf.toString() + this.skeletonAddress.toString()).hashCode();
    }

    public String toStringStub() {
        return this.intf.toString() + this.skeletonAddress.toString();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        Socket socket = null;
        ObjectOutputStream objOutput = null;
        ObjectInputStream objInput = null;

        //System.out.println(method.getName());
        try {
            this.intf.getMethod(method.getName(), method.getParameterTypes());
        }
        catch(NoSuchMethodException e) {
            switch(method.getName()) {
                case "equals":                  return this.equalsStub(args[0]);
                case "hashCode":                return this.hashCodeStub();
                case "toString":                return this.toStringStub();
                default:                        break;
            }
        }

        try {
            // System.out.println("---------------" + this.skeletonAddress.getAddress().toString());

            socket = new Socket(this.skeletonAddress.getAddress(),
                                       this.skeletonAddress.getPort());
            objOutput = new ObjectOutputStream(socket.getOutputStream());
            objOutput.flush();
            objInput = new ObjectInputStream(socket.getInputStream());

            //System.out.println("--------------- Connected!");

            //System.out.println("\n\n---Writing " + method.getName());
            objOutput.writeObject(method.getName());
            //System.out.println("\n\n---Writing " + method.getParameterTypes());
            objOutput.writeObject(method.getParameterTypes());
            //System.out.println("\n\n---Writing " + Arrays.toString(args));
            objOutput.writeObject(args);
            //objOutput.flush();

            result = objInput.readObject();
        }
        catch(UnknownHostException e) {
            //System.out.println("------------ RMIInvocation");
            throw new RMIException(e);
        }
        catch(IOException e) {
            //System.out.println("------------ RMIInvocation");
            throw new RMIException(e);
        }
        finally {
            if(objInput != null) objInput.close();
            if(objOutput != null) objOutput.close();
            if(socket != null) socket.close();
        }
        if(result instanceof InvocationTargetException) {
            throw ((InvocationTargetException) result).getTargetException();
        }
        return result;
    }
}
