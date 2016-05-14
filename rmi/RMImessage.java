package rmi;

import java.io.*;
import java.net.*;
import java.lang.reflect.*;

/**
 * @author: Yilun Zhang, Qiao Zhang
 * @date: 2016/04/26
 */

/** RMI MyInvocationHandler

    <p>
    A class for dynamic proxy.
 */

public class RMImessage implements Serializable{
	private Object proxy = null;
	private Object[] args =null;
	private boolean isException = false;
	private Object returnValue = null;
	private Object exception = null;
	private String methodName = null;
	private Class[] types = null;

	public RMImessage(Object proxy, Method method, Object[] args){
		this.proxy = proxy;
		this.args = args;
		this.methodName = method.getName();
		this.types = method.getParameterTypes();
	}

	public RMImessage(Object proxy, Method method, Object[] args, boolean isException){
		this.proxy = proxy;
		this.methodName = method.getName();
		this.types = method.getParameterTypes();
		this.args = args;
		this.isException = isException;
	}

	public RMImessage(Object returnValue){
		this.returnValue = returnValue;

	}

	public RMImessage(Object exception, boolean isException){
		this.exception = exception;
		this.isException = isException;
	}

	public Object getProxy(){
		return this.proxy;
	}

	public String getMethodName(){
		return this.methodName;
	}

	public Class[] getTypes(){
		return this.types;
	}

	public Object[] getArgs(){
		return this.args;
	}

	public Object getReturnValue(){
		return this.returnValue;
	}

	public boolean getIsException(){
		return this.isException;
	}

	public Object getException(){
		return this.exception;
	}

	public void setException(boolean val){
        isException = val;
	}


}
