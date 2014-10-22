package me.vemacs.friends.data;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FriendsDatabase {
    @Getter
    private static final String prefix = "friends.";

    // Singleton stuff
    private static FriendsDatabase instance;

    protected FriendsDatabase() {

    }

    public static FriendsDatabase getInstance() {
        if (instance == null)
            instance = new FriendsDatabase();
        return instance;
    }

    private JedisPool pool;

    public void init(String ip, int port) {
        pool = new JedisPool(new JedisPoolConfig(), ip, port, 0);
    }

    public void init(String ip, int port, String password) {
        pool = new JedisPool(new JedisPoolConfig(), ip, port, 0, password);
    }

    public Set<User> getOnlineUsers() {
        try (Jedis jedis = getResource()) {
            Set<User> temp = new HashSet<>();
            Set<String> jedisOnline = jedis.smembers(prefix + "currentlyonline");
            for (String s : jedisOnline)
                temp.add(new FriendsUser(UUID.fromString(s)));
            return temp;
        }
    }

    public void addOnlineUser(User user) {
        try (Jedis jedis = getResource()) {
            jedis.sadd(prefix + "currentlyonline", user.getUuid().toString());
        }
    }

    public void removeOnlineUser(User user) {
        try (Jedis jedis = getResource()) {
            jedis.srem(prefix + "currentlyonline", user.getUuid().toString());
        }
    }

    public boolean isUserOnline(User user) {
        try (Jedis jedis = getResource()) {
            return jedis.sismember(prefix + "currentlyonline", user.getUuid().toString());
        }
    }

    public void shutdown() {
        pool.destroy();
    }

    public static Jedis getResource() {
        return instance.pool.getResource();
    }
}