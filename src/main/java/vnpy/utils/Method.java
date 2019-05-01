package vnpy.utils;

import java.lang.reflect.InvocationTargetException;

public class Method {
	private Object instance;
	private java.lang.reflect.Method method;
	
	public Method(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		try {
			//this.method = instance.getClass().getMethod(methodName, parameterTypes);
			this.method = clazz.getDeclaredMethod(methodName, parameterTypes);
			this.method.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new AppException("找不到method");
		}
	}
	
	public Method(Object instance, String methodName, Class<?>... parameterTypes) {
		this.instance = instance;
		try {
			//this.method = instance.getClass().getMethod(methodName, parameterTypes);
			this.method = instance.getClass().getDeclaredMethod(methodName, parameterTypes);
			this.method.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new AppException("找不到method");
		}
	}
	
	public Object invoke(Object... args) {
		Object returnVal = null;
		try {
			returnVal = this.method.invoke(instance, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			throw new AppException("method调用失败");
		}
		return returnVal;
	}
	
	@Override
	public String toString() {
		return this.method.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Method) {
			Method otherMethod = (Method)obj;
			if (this.instance == null) {
				return (otherMethod.getInstance() == null) && this.method.equals(otherMethod.getMethod());
			} else {
				return this.instance.equals(otherMethod.getInstance()) && this.method.equals(otherMethod.getMethod());
			}
		} else {
			return false;
		}
	}
	
	
	///////////////////////Getter Setter////////////////////
	
	public Object getInstance() {
		return instance;
	}

	public void setInstance(Object instance) {
		this.instance = instance;
	}

	public java.lang.reflect.Method getMethod() {
		return method;
	}

	public void setMethod(java.lang.reflect.Method method) {
		this.method = method;
	}

	public static void main(String[] args) {
		
	}
	
}
