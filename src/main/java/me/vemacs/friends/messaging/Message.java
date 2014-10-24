package me.vemacs.friends.messaging;

import com.google.gson.JsonObject;
import lombok.Data;
import me.vemacs.friends.data.FriendsUser;

import java.util.UUID;

@Data
public class Message {
    private final Action action;
    private final UUID sourceUuid;
    private final UUID targetUuid;
    private final JsonObject payload;

    public FriendsUser getSourceUser() {
        return new FriendsUser(sourceUuid);
    }

    public FriendsUser getTargetUser() {
        return new FriendsUser(targetUuid);
    }
}
