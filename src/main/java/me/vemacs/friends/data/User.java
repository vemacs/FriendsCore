package me.vemacs.friends.data;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface User {
    void init();

    void login();

    void logout();

    void setServer(String server);

    Map<User, Boolean> getFriends();

    Set<User> getOnlineFriends();

    void addFriend(User user);

    void removeFriend(User user);

    void addOnlineFriend(User user);

    void removeOnlineFriend(User user);

    boolean hasFriend(User user);

    boolean isOnline();

    long getLastSeen();

    UUID getUuid();
}
