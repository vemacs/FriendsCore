package me.vemacs.friends.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode
public abstract class User {
    @Getter
    protected UUID uuid;

    public User(UUID uuid) {
        this.uuid = uuid;
        init();
    }

    public abstract void init();

    public abstract void login();

    public abstract void logout();

    public abstract Set<User> getFriends();

    public abstract Set<User> getOnlineFriends();

    public abstract void addFriend(User user);

    public abstract void removeFriend(User user);

    public abstract void addOnlineFriend(User user);

    public abstract void removeOnlineFriend(User user);

}
