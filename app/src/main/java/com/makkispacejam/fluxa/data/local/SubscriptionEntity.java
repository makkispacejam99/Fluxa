package com.makkispacejam.fluxa.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "subscriptions",
    indices = {@Index("isBlocked"), @Index("subscribedAt")}
)
public class SubscriptionEntity {
    @PrimaryKey
    @NonNull
    public String channelId;
    public String channelName;
    public String avatarUrl;
    public String subscriberCount;
    public long subscribedAt;
    public boolean isBlocked;

    public SubscriptionEntity(@NonNull String channelId, String channelName, String avatarUrl) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.avatarUrl = avatarUrl;
        this.subscribedAt = System.currentTimeMillis();
    }
}
