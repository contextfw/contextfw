package net.contextfw.web.application.internal;

import java.lang.reflect.Method;

import net.contextfw.web.application.component.Component;
import net.contextfw.web.application.remote.Remoted;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class ComponentUpdateHandlerFactory {
    
    private Gson gson;
    
    @Inject
    public ComponentUpdateHandlerFactory(Injector injector, Gson gson) {
        this.gson = gson;
    }
    
    public ComponentUpdateHandler createHandler(Class<? extends Component> elClass, String methodName) {

        Class<?> cls = elClass;
        Method method = null;

        while (Component.class.isAssignableFrom(cls) && method == null) {
            method = findMethod(cls, methodName);
            cls = cls.getSuperclass();
        }

        if (method != null) {
            return new ComponentUpdateHandler(ComponentUpdateHandler.getKey(elClass, methodName), method, gson);
        }
        else {
            return null;
        }
    }

    private Method findMethod(Class<?> cls, String methodName) {
        for (Method method : cls.getDeclaredMethods()) {
            if (method.getAnnotation(Remoted.class) != null && method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
}