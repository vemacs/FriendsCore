package me.vemacs.friends.data;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Set;

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
        return null;
    }

    public void shutdown() {
        pool.destroy();
    }

    public static Jedis getResource() {
        return instance.pool.getResource();
    }
}