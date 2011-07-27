/*
Redline Smalltalk is licensed under the MIT License

Redline Smalltalk Copyright (c) 2010 James C. Ladd

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Please see DEVELOPER-CERTIFICATE-OF-ORIGIN if you wish to contribute a patch to Redline Smalltalk.
*/
package st.redline;

import java.util.HashMap;
import java.util.Map;

public class ProtoObject {

	private static final Map<String, ProtoObject> classRegistry = new HashMap<String, ProtoObject>();

	private ProtoObjectData data;

	public ProtoObject() {
		this(true);
	}

	public ProtoObject(boolean isClass) {
		data = isClass ? ProtoObjectData.classData() : ProtoObjectData.instanceData();
	}

	public static void primitiveMain(ProtoObject receiver, String[] args) {
	}

	public static ProtoObject primitiveRegisterAs(ProtoObject receiver, String name) {
		classRegistry.put(name, receiver);
		return receiver;
	}

	public static ProtoObject primitiveVariableAt(ProtoObject receiver, String name) {
		if (primitiveIsInstanceVariable(receiver, name))
			throw new IllegalStateException("todo - implement");
		if (primitiveIsClassVariable(receiver, name))
			throw new IllegalStateException("todo - implement");
		return primitiveResolveObject(receiver, name);
	}

	public static boolean primitiveIsInstanceVariable(ProtoObject receiver, String name) {
		return false;
	}

	public static boolean primitiveIsClassVariable(ProtoObject receiver, String name) {
		return false;
	}

	public static ProtoObject primitiveSymbol(ProtoObject receiver, String value) {
		ProtoObject symbolClass = receiver.resolveObject("Symbol");  // <- should we do primitiveVariableAt to namespaces are used?
		ProtoObject symbol = new ProtoObject(false);
		symbol.cls(symbolClass);
		symbol.javaValue(value);
		return symbol;
	}

	public static ProtoObject primitiveSend(ProtoObject receiver, String selector, ProtoObject classMethodWasFoundIn) {
		throw new IllegalStateException("todo - implement " + selector);
	}

	public static ProtoObject primitiveSend(ProtoObject receiver, ProtoObject arg1, String selector, ProtoObject classMethodWasFoundIn) {
		ProtoMethod method = receiver.cls().methodAt(selector);
		if (method != null)
			return method.applyTo(receiver, receiver.cls(), arg1);
		ProtoObject[] newClassMethodWasFoundIn = {null};
		method = methodFor(receiver.cls().superclass(), selector, newClassMethodWasFoundIn);
		if (method != null)
			return method.applyTo(receiver, newClassMethodWasFoundIn[0], arg1);
		if (isBootstrapped(receiver))
			if (resolveClassObject(receiver))
				return primitiveSend(receiver, arg1, selector, classMethodWasFoundIn);
		return sendDoesNotUnderstand(receiver, selector, new ProtoObject[] {arg1});
	}

	private static boolean isBootstrapped(ProtoObject object) {
		return (object != null && (object.isBootstrapped() || object.cls().isBootstrapped()));
	}

	public ProtoObject ping() {
		System.out.println("here1 " + this + " " + this.cls() + " Proto  " + classRegistry.get("ProtoObject") + " symbol " + classRegistry.get("Symbol"));
		return this;
	}

	private static boolean isNotBootstrapped(ProtoObject object) {
		return !isBootstrapped(object);
	}

	private static boolean resolveClassObject(ProtoObject object) {
		return resolveClassObject(object, null);
	}

	private static boolean resolveClassObject(ProtoObject object, ProtoObject other) {
		System.out.println("resolveClassObject() " + object + " " + other);
		if (!resolveObjectSuperclass(object, other))
			return false;
		if (isNotBootstrapped(object))
			return true;
		return primitiveResolveObject(object, "Foo") != null;
	}

	private static boolean resolveObjectSuperclass(ProtoObject object, ProtoObject other) {
		if (object == null)
			return true;
		if (!object.isClass())
			return resolveObjectSuperclass(object.cls(), other);
		ProtoObject superclass = object.superclass();
		if (superclass != other)
			return resolveClassObject(superclass, other);
		ProtoObject metaclass = object.cls().superclass();
		return resolveClassObject(metaclass, metaclass);
	}

	private static ProtoObject sendDoesNotUnderstand(ProtoObject receiver, String selector, ProtoObject[] arguments) {
		throw new RuntimeException("TODO -  need to implement send of doesNotUnderstand - '" + selector + "'");
	}

	private static ProtoMethod methodFor(ProtoObject object, String selector, ProtoObject[] classMethodFoundIn) {
		ProtoMethod method;
		ProtoObject superclass = object;
		while ((method = superclass.methodAt(selector)) == null)
			if ((superclass = superclass.superclass()) == null)
				break;
		classMethodFoundIn[0] = superclass;
		return method;
	}

	public static ProtoObject primitiveResolveObject(ProtoObject receiver, String name) {
		ProtoObject object = receiver.resolveObject(name);
		if (object != null)
			return object;
		// TODO.JCL search through namespaces to find object.
		return receiver.resolveObject("st.redline." + name);
	}

	private ProtoObject resolveObject(String name) {
		System.out.println("resolveObject() " + name);
		if (classRegistry.containsKey(name))
			return classRegistry.get(name);
		// It is expected the loading of an object results in the registering a Smalltalk class in the class registry.
		// *NOTE* if class is not registered the will be a NullPointerException as we return 'null' here.
		if (loadObject(name))
			return classRegistry.get(name);
		// TODO.JCL should we return 'nil'?
		throw new IllegalStateException("Handle this - Class not found.");
	}

	private boolean loadObject(String name) {
		try {
			return Class.forName(name, true, classLoader()).newInstance() != null;
		} catch (Exception e) {
			return false;
		}
	}

	private ClassLoader classLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	public void bootstrap() {
		new Bootstrapper(this).bootstrap();
	}

	public void bootstrapped() {
		data.bootstrapped();
	}

	public boolean isBootstrapped() {
		return data.isBootstrapped();
	}

	protected void cls(ProtoObject cls) {
		data.cls(cls);
	}

	protected ProtoObject cls() {
		return data.cls();
	}

	protected void superclass(ProtoObject superclass) {
		data.superclass(superclass);
	}

	protected ProtoObject superclass() {
		return data.superclass();
	}

	protected void javaValue(String value) {
		data.javaValue(value);
	}

	public String javaValue() {
		return data.javaValue();
	}

	protected ProtoMethod methodAt(String selector) {
		return data.methodAt(selector);
	}

	protected void methodAtPut(String selector, ProtoMethod method) {
		data.methodAtPut(selector, method);
	}

	protected boolean isClass() {
		return data.isClass();
	}
}