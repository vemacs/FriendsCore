package me.vemacs.friends.messaging;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FriendsHandlerList {
    private static FriendsHandlerList instance;

    protected FriendsHandlerList() {

    }

    public static FriendsHandlerList getInstance() {
        if (instance == null)
            instance = new FriendsHandlerList();
        return instance;
    }

    // jesus fuck this is convoluted
    private Map<ActionListener, Map<Action, List<Method>>> registeredListeners = new HashMap<>();
    private Class[] validParam = {Message.class};

    public Map<Action, List<Method>> getAllActionHandlersFor(Class listenerClass) {
        Map<Action, List<Method>> methodMap = new HashMap<>();
        for (Method m : listenerClass.getMethods()) { // we only want public methods
            Action action = getActionForMethod(m);
            if (action == null) continue;
            if (!Arrays.equals(m.getParameterTypes(), validParam)) continue;
            List<Method> methodList = methodMap.get(action);
            if (methodList == null) methodMap.put(action, methodList = new CopyOnWriteArrayList<>());
            methodList.add(m);
        }
        return methodMap;
    }

    public Action getActionForMethod(Method method) {
        if (method.isAnnotationPresent(ActionHandler.class))
            return method.getAnnotation(ActionHandler.class).action();
        return null;
    }

    public void registerListener(ActionListener listener) {
        registeredListeners.put(listener, getAllActionHandlersFor(listener.getClass()));
    }

    public void unregisterListener(ActionListener listener) {
        registeredListeners.remove(listener);
    }

    public void handle(Action action, Message payload) {
        for (Map.Entry<ActionListener, Map<Action, List<Method>>> entry : registeredListeners.entrySet()) {
            ActionListener al = entry.getKey();
            if (!entry.getValue().containsKey(action)) continue;
            for (Method m : entry.getValue().get(action)) {
                try {
                    m.invoke(al, payload);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
