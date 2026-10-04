package framework;

import java.lang.reflect.Method;

public class VerbAction {
    private Object controllerInstance;
    private Method method;

    public VerbAction(Object controllerInstance, Method method) {
        this.controllerInstance = controllerInstance;
        this.method = method;
    }

    public Object getControllerInstance() {
        return controllerInstance;
    }

    public Method getMethod() {
        return method;
    }
}