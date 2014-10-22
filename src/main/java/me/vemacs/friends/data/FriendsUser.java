package me.vemacs.friends.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.vemacs.friends.messaging.Action;
import me.vemacs.friends.messaging.ActionDispatcher;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode
public class FriendsUser implements User {
    private UUID uuid;

    public FriendsUser(UUID uuid) {
        this.uuid = uuid;
        init();
    }

    private FriendsDatabase db = FriendsDatabase.getInstance();
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
        db.addOnlineUser(this);
        Set<User> intersection = new HashSet<>(getFriends());
        intersection.retainAll(db.getOnlineUsers());
        for (User friend : intersection) {
            addOnlineFriend(friend);
            friend.addOnlineFriend(this);
            ActionDispatcher.getInstance().dispatchAction(Action.LOGIN, friend, this);
        }
    }

    @Override
    public void logout() {
        db.removeOnlineUser(this);
        for (User friend : getOnlineFriends()) {
            friend.removeOnlineFriend(this);
            ActionDispatcher.getInstance().dispatchAction(Action.LOGOUT, friend, this);
        }
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.del(onlineSet);
        }
    }

    @Override
    public Set<User> getFriends() {
        Set<User> friends = new HashSet<>();
        try (Jedis jedis = FriendsDatabase.getResource()) {
            for (String str : jedis.smembers(friendsSet))
                friends.add(new FriendsUser(UUID.fromString(str)));
        }
        return friends;
    }

    @Override
    public Set<User> getOnlineFriends() {
        Set<User> friends = new HashSet<>();
        try (Jedis jedis = FriendsDatabase.getResource()) {
            for (String str : jedis.smembers(onlineSet))
                friends.add(new FriendsUser(UUID.fromString(str)));
        }
        return friends;
    }

    @Override
    public void addFriend(User friend) {
        try (Jedis jedis = FriendsDatabase.getResource()) {
            if (jedis.sadd(friendsSet, friend.getUuid().toString()) == 0) return;
        }
        friend.addFriend(this);
        addOnlineFriend(friend);
        ActionDispatcher.getInstance().dispatchAction(Action.FRIEND_ADD, this, friend);
    }

    @Override
    public void removeFriend(User friend) {
        if (!hasFriend(friend)) return;
        try (Jedis jedis = FriendsDatabase.getResource()) {
            if (jedis.srem(friendsSet, friend.getUuid().toString()) == 0) return;
        }
        friend.removeFriend(this);
        removeOnlineFriend(friend);
        ActionDispatcher.getInstance().dispatchAction(Action.FRIEND_REMOVE, this, friend);
    }

    @Override
    public void addOnlineFriend(User friend) {
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.sadd(onlineSet, friend.getUuid().toString());
        }
        ActionDispatcher.getInstance().dispatchAction(Action.ONLINE_ADD, this, friend);
    }

    @Override
    public void removeOnlineFriend(User friend) {
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.srem(onlineSet, friend.getUuid().toString());
        }
        ActionDispatcher.getInstance().dispatchAction(Action.ONLINE_REMOVE, this, friend);
    }

    @Override
    public boolean hasFriend(User friend) {
        try (Jedis jedis = FriendsDatabase.getResource()) {
            return jedis.sismember(friendsSet, friend.getUuid().toString());
        }
    }

    @Override
    public boolean isOnline() {
        return db.isUserOnline(this);
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }
}
