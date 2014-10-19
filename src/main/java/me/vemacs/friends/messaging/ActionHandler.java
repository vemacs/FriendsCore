package me.vemacs.friends.messaging;

import me.vemacs.friends.data.User;

public interface ActionHandler {
    public Action getAction();

    public void handle(User recipient, User subject);
}
