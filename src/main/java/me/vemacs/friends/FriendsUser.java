package me.vemacs.friends;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FriendsUser extends User {
    public FriendsUser(UUID uuid) {
        super(uuid);
    }

    private FriendsDatabase db = FriendsDatabase.getInstance();
    private JedisPool pool = FriendsDatabase.getInstance().getPool();

    private final String prefix = FriendsDatabase.getPrefix() + "user.";

    private String friendsSet;
    private String onlineSet;

    @Override
    public void init() {
        friendsSet = getFriendsSetName();
        onlineSet = getOnlineSetName();
    }

    public String getFriendsSetName() {
        return prefix + uuid.toString() + ".friends";
    }

    public String getOnlineSetName() {
        return prefix + uuid.toString() + ".online";
    }

    @Override
    public void login() {
        Set<User> intersection = new HashSet<>(getFriends());
        intersection.retainAll(db.getOnlineUsers());
        for (User friend : intersection) {
            addOnlineFriend(friend);
        }
    }

    @Override
    public void logout() {
        for (User friend : getOnlineFriends()) {
            friend.removeOnlineFriend(this);
        }
        try (Jedis jedis = pool.getResource()) {
            jedis.del(onlineSet);
        }
    }

    @Override
    public Set<User> getFriends() {
        Set<User> friends = new HashSet<>();
        try (Jedis jedis = pool.getResource()) {
            for (String str : jedis.smembers(friendsSet))
                friends.add(new FriendsUser(UUID.fromString(str)));
        }
        return friends;
    }

    @Override
    public Set<User> getOnlineFriends() {
        Set<User> friends = new HashSet<>();
        try (Jedis jedis = pool.getResource()) {
            for (String str : jedis.smembers(onlineSet))
                friends.add(new FriendsUser(UUID.fromString(str)));
        }
        return friends;
    }

    @Override
    public void addFriend(User friend) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(friendsSet, friend.getUuid().toString());
        }
        addOnlineFriend(friend);
        friend.addOnlineFriend(this);
    }

    @Override
    public void removeFriend(User friend) {
        try (Jedis jedis = pool.getResource()) {
            jedis.srem(friendsSet, friend.getUuid().toString());
        }
        removeOnlineFriend(friend);
        friend.removeOnlineFriend(this);
    }

    @Override
    public void addOnlineFriend(User friend) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(onlineSet, friend.getUuid().toString());
        }
        friend.addOnlineFriend(this);
    }

    @Override
    public void removeOnlineFriend(User friend) {
        try (Jedis jedis = pool.getResource()) {
            jedis.srem(onlineSet, friend.getUuid().toString());
        }
        friend.removeOnlineFriend(this);
    }
}
