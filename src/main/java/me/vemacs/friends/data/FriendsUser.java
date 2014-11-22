package me.vemacs.friends.data;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import me.vemacs.friends.messaging.Action;
import me.vemacs.friends.messaging.ActionDispatcher;
import me.vemacs.friends.messaging.Message;
import redis.clients.jedis.Jedis;

import java.util.*;

@EqualsAndHashCode
public class FriendsUser implements User {
    private UUID uuid;

    public FriendsUser(UUID uuid) {
        this.uuid = uuid;
        init();
    }

    private FriendsDatabase db = FriendsDatabase.getInstance();
    private static final String prefix = FriendsDatabase.getPrefix() + "user.";

    private String friendsSet;
    private String onlineSet;
    private String lastSeen;
    private static final String serverHash = FriendsDatabase.getPrefix() + "userservers";
    private String server;

    @Override
    public void init() {
        friendsSet = getFriendsSetName();
        onlineSet = getOnlineSetName();
        lastSeen = getLastSeenName();
    }

    public String getFriendsSetName() {
        return prefix + uuid.toString() + ".friends";
    }

    public String getOnlineSetName() {
        return prefix + uuid.toString() + ".online";
    }

    public String getLastSeenName() {
        return prefix + uuid.toString() + ".lastseen";
    }

    @Override
    public void login() {
        db.addOnlineUser(this);
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.set(lastSeen, Long.toString(System.currentTimeMillis()));
            Set<User> intersection = new HashSet<>();
            Set<String> tmp = jedis.sinter(friendsSet, FriendsDatabase.getPrefix() +
                    FriendsDatabase.getOnlineKey());
            for (String s : tmp)
                intersection.add(new FriendsUser(UUID.fromString(s)));
            for (User friend : intersection) {
                addOnlineFriend(friend);
                friend.addOnlineFriend(this);
                ActionDispatcher.getInstance().dispatchAction(new Message(Action.LOGIN, friend.getUuid(), uuid, null));
            }
        }
    }

    @Override
    public void logout() {
        db.removeOnlineUser(this);
        for (User friend : getOnlineFriends()) {
            friend.removeOnlineFriend(this);
            ActionDispatcher.getInstance().dispatchAction(new Message(Action.LOGOUT, friend.getUuid(), uuid, null));
        }
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.set(lastSeen, Long.toString(System.currentTimeMillis()));
            jedis.hdel(serverHash, uuid.toString());
            jedis.del(onlineSet);
        }
    }

    @Override
    public void setServer(String server) {
        String oldServer = server;
        this.server = server;
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.hset(serverHash, this.uuid.toString(), server);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("old", oldServer);
        payload.addProperty("new", server);

        for (User user : getOnlineFriends())
        {
            ActionDispatcher.getInstance().dispatchAction(new Message(Action.CHANGE_SERVER, uuid, user.getUuid(), payload));
        }
    }

    @Override
    public Map<User, Boolean> getFriends() {
        Map<User, Boolean> friends = new LinkedHashMap<>();
        try (Jedis jedis = FriendsDatabase.getResource()) {
            for (User u : getOnlineFriends())
                friends.put(u, true);

            Map<Long, User> tmpMap = new TreeMap<>(Collections.reverseOrder());
            for (String str : jedis.smembers(friendsSet)) {
                User tmpUser = new FriendsUser(UUID.fromString(str));
                if (!friends.containsKey(tmpUser))
                    tmpMap.put(tmpUser.getLastSeen(), tmpUser);
            }

            for (User user : tmpMap.values()) {
                friends.put(user, false);
            }
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
        ActionDispatcher.getInstance().dispatchAction(new Message(Action.FRIEND_ADD, this.uuid, friend.getUuid(), null));
    }

    @Override
    public void removeFriend(User friend) {
        if (!hasFriend(friend)) return;
        try (Jedis jedis = FriendsDatabase.getResource()) {
            if (jedis.srem(friendsSet, friend.getUuid().toString()) == 0) return;
        }
        friend.removeFriend(this);
        removeOnlineFriend(friend);
        ActionDispatcher.getInstance().dispatchAction(new Message(Action.FRIEND_REMOVE, this.uuid, friend.getUuid(), null));
    }

    @Override
    public void addOnlineFriend(User friend) {
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.sadd(onlineSet, friend.getUuid().toString());
        }
        ActionDispatcher.getInstance().dispatchAction(new Message(Action.ONLINE_ADD, this.uuid, friend.getUuid(), null));
    }

    @Override
    public void removeOnlineFriend(User friend) {
        try (Jedis jedis = FriendsDatabase.getResource()) {
            jedis.srem(onlineSet, friend.getUuid().toString());
        }
        ActionDispatcher.getInstance().dispatchAction(new Message(Action.ONLINE_REMOVE, this.uuid, friend.getUuid(), null));
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
    public long getLastSeen() {
        try (Jedis jedis = FriendsDatabase.getResource()) {
            String tmp = jedis.get(lastSeen);
            if (tmp == null) return 0;
            return Long.parseLong(tmp);
        }
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }
}
