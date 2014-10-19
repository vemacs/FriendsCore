package me.vemacs.friends;

import lombok.Getter;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionDispatcher {
    private static ActionDispatcher instance;
    protected ActionDispatcher() {

    }

    public static ActionDispatcher getInstance() {
        if (instance == null)
            instance = new ActionDispatcher();
        return instance;
    }

    @Getter
    private static final String channelPrefix = "fl-";

    // hurrdurr no multimap
    private Map<Action, List<ActionHandler>> registeredHandlers = new ConcurrentHashMap<>();

    public void dispatchAction(Action action, User recipient, User subject) {
        try (Jedis jedis = FriendsDatabase.getInstance().getPool().getResource()) {
            jedis.publish(channelPrefix + action.name().toLowerCase(),
                    recipient.getUuid().toString() + "," + subject.getUuid().toString());
        }
    }

    public void handle(String channel, String message) {
        Action action = Action.valueOf(channel.substring(channelPrefix.length()).toUpperCase());
        String[] parts = message.split(",");
        for (ActionHandler handler : registeredHandlers.get(action))
            handler.handle(new FriendsUser(UUID.fromString(parts[0])), new FriendsUser(UUID.fromString(parts[1])));
    }

    public void register(ActionHandler handler) {
        if (!registeredHandlers.containsKey(handler.getAction()))
            registeredHandlers.put(handler.getAction(), new ArrayList<ActionHandler>());
        registeredHandlers.get(handler.getAction()).add(handler);
    }

    public void unregister(ActionHandler handler) {
        if (!registeredHandlers.containsKey(handler.getAction()))
            registeredHandlers.put(handler.getAction(), new ArrayList<ActionHandler>());
        registeredHandlers.get(handler.getAction()).remove(handler);
    }
}
