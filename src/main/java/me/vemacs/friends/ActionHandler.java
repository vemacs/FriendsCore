package me.vemacs.friends;

public interface ActionHandler {
    public Action getAction();

    public void handle(User recipient, User subject);
}
