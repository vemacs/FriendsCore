package me.vemacs.friends.data;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface User {
    public abstract void init();

    public abstract void login();

    public abstract void logout();

    public abstract Map<Boolean, User> getFriends();

    public abstract Set<User> getOnlineFriends();

    public abstract void addFriend(User user);

    public abstract void removeFriend(User user);

    public abstract void addOnlineFriend(User user);

    public abstract void removeOnlineFriend(User user);

    public abstract boolean hasFriend(User user);

    public abstract boolean isOnline();

    public abstract long getLastSeen();

    public abstract UUID getUuid();
}
