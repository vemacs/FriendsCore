package me.vemacs.friends.messaging;

public interface ActionHandler {
    Action getAction();

    void handle(Message payload);
}
