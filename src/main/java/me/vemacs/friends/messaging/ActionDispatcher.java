package me.vemacs.friends.messaging;

import com.google.gson.Gson;
import lombok.Getter;
import me.vemacs.friends.data.FriendsDatabase;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final String channel = "friendscore";

    private static final Gson gson = new Gson();

    public void init() {
        final JedisPubSub pubSubHandler = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handle(channel, message);
            }

            @Override
            public void onPMessage(String s, String s2, String s3) {

            }

            @Override
            public void onSubscribe(String s, int i) {

            }

            @Override
            public void onUnsubscribe(String s, int i) {

            }

            @Override
            public void onPUnsubscribe(String s, int i) {

            }

            @Override
            public void onPSubscribe(String s, int i) {

            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                FriendsDatabase.getResource().subscribe(pubSubHandler, channel);
            }
        }).start();
    }

    // hurrdurr no multimap
    private Map<Action, List<ActionHandler>> registeredHandlers = new ConcurrentHashMap<>();

    public void dispatchAction(Message message) {
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.publish(channel, gson.toJson(message));
        }
    }

    public void handle(String channel, String message) {
        Message message1 = gson.fromJson(message, Message.class);
        if (registeredHandlers.containsKey(message1.getAction()))
            for (ActionHandler handler : registeredHandlers.get(message1.getAction()))
                handler.handle(message1);
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
