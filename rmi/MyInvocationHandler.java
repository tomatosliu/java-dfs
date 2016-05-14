package rmi;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.*;
import java.lang.Exception.*;

/**
 * @author: Yilun Zhang, Qiao Zhang
 * @date: 2016/04/26
 */

/** RMI MyInvocationHandler

    <p>
    A class for dynamic proxy.
 */

public class MyInvocationHandler implements InvocationHandler,Serializable{
  private InetSocketAddress address;
  private Class c;

  public MyInvocationHandler(InetSocketAddress address, Class c){
       this.address = address;
       this.c = c;
  }

  public InetSocketAddress getAddress(){
    return this.address;
  }

  public Class getC(){
    return this.c;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args)throws Throwable{

    if(method.getName().equals("toString") && method.getReturnType().getName().equals("java.lang.String")
      && method.getParameterTypes().length == 0) {
      MyInvocationHandler temp = (MyInvocationHandler)Proxy.getInvocationHandler(proxy);

      return temp.getC().getName() + " " + temp.getAddress().toString();
    }

    if(method.getName().equals("hashCode") && method.getReturnType().getName().equals("int") 
      && method.getParameterTypes().length == 0) {

      MyInvocationHandler temp = (MyInvocationHandler)Proxy.getInvocationHandler(proxy);

      return temp.getC().hashCode() * temp.getAddress().hashCode();
    }


    if (method.toString().equals("public boolean java.lang.Object.equals(java.lang.Object)")) {
      Object obj = args[0];
      if (obj == null)
        return false;
      if (obj instanceof Proxy) {
        MyInvocationHandler stub1 = (MyInvocationHandler) Proxy.getInvocationHandler(proxy);
        MyInvocationHandler stub2 = (MyInvocationHandler) Proxy.getInvocationHandler(obj);
                
        if ((stub1.getC().equals(stub2.getC())) && (stub1.getAddress().equals(stub2.getAddress()))) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    }
    RMImessage requestMsg = null;
    Socket sock = null;
    RMImessage responseMsg = null;

    try{

      sock = new Socket(address.getAddress(),address.getPort());

    	ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
    	out.flush();
    	ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

      requestMsg = new RMImessage(proxy,method,args);

      out.writeObject(requestMsg);

    	responseMsg = (RMImessage) in.readObject();
      sock.close();

    }catch(Exception e){
      throw new RMIException(e.getCause());
    }

    if(responseMsg.getIsException()){
      throw (Exception)responseMsg.getException();
    }else{
      return responseMsg.getReturnValue();
    }

  }
}
