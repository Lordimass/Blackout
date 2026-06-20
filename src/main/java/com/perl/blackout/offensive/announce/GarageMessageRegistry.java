package com.perl.blackout.offensive.announce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class GarageMessageRegistry implements JsonAssetWithMap<String, DefaultAssetMap<String, GarageMessageRegistry>> {

    public static final AssetBuilderCodec<String, GarageMessageRegistry> CODEC;
    private static AssetStore<String, GarageMessageRegistry, DefaultAssetMap<String, GarageMessageRegistry>> ASSET_STORE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected String category = "Generic";
    protected String delivery = "Toast";
    protected String style = "Default";
    protected boolean major = false;
    protected String icon;
    protected String soundEventId;
    protected float durationSeconds = 4.0f;
    protected ArrayList<Entry> entries = new ArrayList<>();

    private GarageMessageRegistry() {
    }

    public static AssetStore<String, GarageMessageRegistry, DefaultAssetMap<String, GarageMessageRegistry>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(GarageMessageRegistry.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, GarageMessageRegistry> getAssetMap() {
        return (DefaultAssetMap<String, GarageMessageRegistry>) getAssetStore().getAssetMap();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getCategory() {
        return this.category;
    }

    public String getDelivery() {
        return this.delivery;
    }

    public String getStyle() {
        return this.style;
    }

    public boolean isMajor() {
        return this.major;
    }

    @Nullable
    public String getIcon() {
        return this.icon;
    }

    @Nullable
    public String getSoundEventId() {
        return this.soundEventId;
    }

    public float getDurationSeconds() {
        return this.durationSeconds;
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(GarageMessageRegistry.class, GarageMessageRegistry::new, Codec.STRING,
                        (a, s) -> a.id = s, a -> a.id,
                        (a, d) -> a.data = d, a -> a.data)
                .<String>appendInherited(new KeyedCodec<>("Category", Codec.STRING),
                        (a, v) -> a.category = v, a -> a.category, (a, p) -> a.category = p.category)
                .add()
                .<String>appendInherited(new KeyedCodec<>("Delivery", Codec.STRING),
                        (a, v) -> a.delivery = v, a -> a.delivery, (a, p) -> a.delivery = p.delivery)
                .add()
                .<String>appendInherited(new KeyedCodec<>("Style", Codec.STRING),
                        (a, v) -> a.style = v, a -> a.style, (a, p) -> a.style = p.style)
                .add()
                .<Boolean>appendInherited(new KeyedCodec<>("Major", Codec.BOOLEAN),
                        (a, v) -> a.major = v, a -> a.major, (a, p) -> a.major = p.major)
                .add()
                .<String>appendInherited(new KeyedCodec<>("Icon", Codec.STRING),
                        (a, v) -> a.icon = v, a -> a.icon, (a, p) -> a.icon = p.icon)
                .add()
                .<String>appendInherited(new KeyedCodec<>("SoundEventId", Codec.STRING),
                        (a, v) -> a.soundEventId = v, a -> a.soundEventId, (a, p) -> a.soundEventId = p.soundEventId)
                .add()
                .<Float>appendInherited(new KeyedCodec<>("DurationSeconds", Codec.FLOAT),
                        (a, v) -> a.durationSeconds = v, a -> a.durationSeconds, (a, p) -> a.durationSeconds = p.durationSeconds)
                .add()
                .appendInherited(new KeyedCodec<>("Entries", new ArrayCodec<>(Entry.CODEC, Entry[]::new)),
                        (a, v) -> a.entries = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
                        a -> a.entries.toArray(Entry[]::new),
                        (a, p) -> a.entries = new ArrayList<>(p.entries))
                .add()
                .build();
    }

    public static final class Entry {

        public static final BuilderCodec<Entry> CODEC =
                BuilderCodec.builder(Entry.class, Entry::new)
                        .append(new KeyedCodec<>("At", Codec.FLOAT), (e, v) -> e.at = v, e -> e.at)
                        .add()
                        .append(new KeyedCodec<>("TitleKey", Codec.STRING), (e, v) -> e.titleKey = v, e -> e.titleKey)
                        .add()
                        .append(new KeyedCodec<>("BodyKey", Codec.STRING), (e, v) -> e.bodyKey = v, e -> e.bodyKey)
                        .add()
                        .append(new KeyedCodec<>("Delivery", Codec.STRING), (e, v) -> e.delivery = v, e -> e.delivery)
                        .add()
                        .append(new KeyedCodec<>("Style", Codec.STRING), (e, v) -> e.style = v, e -> e.style)
                        .add()
                        .append(new KeyedCodec<>("Icon", Codec.STRING), (e, v) -> e.icon = v, e -> e.icon)
                        .add()
                        .append(new KeyedCodec<>("SoundEventId", Codec.STRING), (e, v) -> e.soundEventId = v, e -> e.soundEventId)
                        .add()
                        .build();

        private float at;
        private String titleKey;
        private String bodyKey;
        private String delivery;
        private String style;
        private String icon;
        private String soundEventId;

        public float getAt() {
            return this.at;
        }

        @Nullable
        public String getTitleKey() {
            return this.titleKey;
        }

        @Nullable
        public String getBodyKey() {
            return this.bodyKey;
        }

        @Nullable
        public String getDelivery() {
            return this.delivery;
        }

        @Nullable
        public String getStyle() {
            return this.style;
        }

        @Nullable
        public String getIcon() {
            return this.icon;
        }

        @Nullable
        public String getSoundEventId() {
            return this.soundEventId;
        }
    }
}
