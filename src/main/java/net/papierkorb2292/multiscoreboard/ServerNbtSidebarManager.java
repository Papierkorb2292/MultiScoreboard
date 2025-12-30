package net.papierkorb2292.multiscoreboard;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.papierkorb2292.multiscoreboard.mixin.BlockDataAccessorAccessor;
import net.papierkorb2292.multiscoreboard.mixin.EntityDataAccessorAccessor;
import net.papierkorb2292.multiscoreboard.mixin.StorageDataAccessorAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServerNbtSidebarManager extends SavedData {

    private static final String ENTITY_DATA_OBJECT_TYPE = "entity";
    private static final String BLOCK_DATA_OBJECT_TYPE = "block";
    private static final String STORAGE_DATA_OBJECT_TYPE = "storage";

    public static final NbtPathArgument.NbtPath ROOT_PATH;

    private static final Codec<Map<String, Entry>> ENTRY_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC);

    static {
        try {
            ROOT_PATH = new NbtPathArgument().parse(new StringReader(""));
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final DynamicCommandExceptionType INVALID_DATA_SOURCE_EXCEPTION = new DynamicCommandExceptionType(arg -> Component.nullToEmpty("Unknown data source: " + arg));

    private final MinecraftServer server;
    private final Map<String, Entry> entries;

    public ServerNbtSidebarManager(MinecraftServer server) {
        this(server, new HashMap<>());
    }

    public ServerNbtSidebarManager(MinecraftServer server, Map<String, Entry> entries) {
        this.server = server;
        this.entries = new HashMap<>(entries);
        tick(); // Make sure to populate all lastSentNbt fields
    }

    public void tick() {
        for(var entry : entries.entrySet()) {
            entry.getValue().updateNbt(entry.getKey(), this);
        }
    }

    /**
     * Adds a new NBT sidebar.
     * @param name The name of the sidebar. If null, a default name will be used consisting of the dataObject and path
     * @param dataObject The 9data source object to get the NBT data from.
     * @param path The path to the NBT data.
     * @return The name of the added sidebar.
     * @throws CommandSyntaxException If the data source object is not a valid data source.
     */
    public String addEntry(@Nullable String name, DataAccessor dataObject, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
        SidebarNBTProvider sidebarNBTProvider;
        if(dataObject instanceof EntityDataAccessorAccessor entityDataObjectAccessor) {
            var uuid = entityDataObjectAccessor.getEntity().getUUID();
            sidebarNBTProvider = new EntitySidebarNbtProvider(uuid);
        } else if(dataObject instanceof BlockDataAccessorAccessor blockDataObjectAccessor) {
            var pos = blockDataObjectAccessor.getPos();
            var worldKey = Objects.requireNonNull(blockDataObjectAccessor.getEntity().getLevel()).dimension();
            sidebarNBTProvider = new BlockSidebarNbtProvider(pos, worldKey);
        } else if(dataObject instanceof StorageDataAccessorAccessor storageDataObjectAccessor) {
            var id = storageDataObjectAccessor.getId();
            sidebarNBTProvider = new StorageSidebarNbtProvider(id);
        } else {
            throw INVALID_DATA_SOURCE_EXCEPTION.create(dataObject.getClass().getName());
        }
        if(name == null) {
            name = sidebarNBTProvider.getDefaultNamePrefix() + (path.toString().isEmpty() ? "" : ("_" + path));
        }
        addEntry(name, sidebarNBTProvider, path);
        return name;
    }

    private void addEntry(String name, SidebarNBTProvider sidebarNBTProvider, NbtPathArgument.NbtPath path) {
        setDirty();
        var entry = new Entry(sidebarNBTProvider, path);
        entries.put(name, entry);
        entry.updateNbt(name, this);
    }

    public boolean removeEntry(String name) {
        setDirty();
        var removedPacket = new RemoveNbtSidebarS2CPacket(name);
        for(var player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, removedPacket);
        }
        return entries.remove(name) != null;
    }

    /**
     * Removes all entries that take their dataSource from given dataSource object.
     * @param dataObject The dataSource object to remove entries for.
     * @return The number of removed entries.
     */
    public int removeEntriesOfDataObject(DataAccessor dataObject) {
        var entryCount = entries.size();
        entries.entrySet().removeIf(entry -> {
            var remove = entry.getValue().nbtProvider.isDataObject(dataObject);
            if(remove) {
                var removedPacket = new RemoveNbtSidebarS2CPacket(entry.getKey());
                for(var player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, removedPacket);
                }
            }
            return remove;
        });
        if(entryCount != entries.size()) setDirty();
        return entryCount - entries.size();
    }

    public int removeAllEntries() {
        var count = entries.size();
        for(var name : entries.keySet()) {
            var removedPacket = new RemoveNbtSidebarS2CPacket(name);
            for(var player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, removedPacket);
            }
        }
        entries.clear();
        setDirty();
        return count;
    }

    public String getEntryNameIfMatches(String name, DataAccessor dataObject, NbtPathArgument.NbtPath path) {
        if(name == null) {
            for(var entry : entries.entrySet()) {
                if(entry.getValue().nbtProvider.isDataObject(dataObject) && entry.getValue().path.toString().equals(path.toString())) {
                    return entry.getKey();
                }
            }
            return null;
        }
        var entry = entries.get(name);
        if(entry != null && entry.nbtProvider.isDataObject(dataObject) && entry.path.toString().equals(path.toString()))
            return name;
        return null;
    }

    public Set<String> getNames() {
        return entries.keySet();
    }

    public void onPlayerJoin(PacketSender packetSender) {
        for(var entry : entries.entrySet()) {
            var packet = new SetNbtSidebarS2CPacket(entry.getKey(), entry.getValue().getLastSentNbt());
            packetSender.sendPacket(packet);
        }
    }

    public static SavedDataType<ServerNbtSidebarManager> getPersistentStateType(MinecraftServer server) {
        return new SavedDataType<>(
                "multiscoreboard_nbt",
                () -> new ServerNbtSidebarManager(server),
                ENTRY_MAP_CODEC.fieldOf("entries").codec().xmap(entries -> new ServerNbtSidebarManager(server, entries), manager -> manager.entries),
                null
        );
    }

    public static final class Entry {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SidebarNBTProvider.CODEC.fieldOf("nbtProvider").forGetter(Entry::getNbtProvider),
                ExtraCodecs.catchDecoderException(Codec.STRING.flatXmap(
                        string -> {
                            try {
                                return DataResult.success(NbtPathArgument.nbtPath().parse(new StringReader(string)));
                            } catch(CommandSyntaxException e) {
                                return DataResult.error(e::getMessage);
                            }
                        },
                        path -> DataResult.success(path.toString())
                )).fieldOf("path").forGetter(Entry::getPath)
        ).apply(instance, Entry::new));

        private final SidebarNBTProvider nbtProvider;
        private final NbtPathArgument.NbtPath path;
        private List<Tag> lastSentNbt = null;

        public Entry(SidebarNBTProvider nbtProvider, NbtPathArgument.NbtPath path) {
            this.nbtProvider = nbtProvider;
            this.path = path;
        }

        public SidebarNBTProvider getNbtProvider() {
            return nbtProvider;
        }

        public NbtPathArgument.NbtPath getPath() {
            return path;
        }

        public List<Tag> getLastSentNbt() {
            return lastSentNbt;
        }

        private void updateNbt(String name, ServerNbtSidebarManager manager) {
            List<Tag> newNBT;
            try {
                var fullNbt = nbtProvider.getNbt(manager.server.registryAccess(), manager);
                newNBT = fullNbt == null ? Collections.emptyList() : path.get(fullNbt);
            } catch(CommandSyntaxException e) {
                newNBT = Collections.emptyList();
            }
            if(!newNBT.equals(lastSentNbt)) {
                lastSentNbt = newNBT.stream().map(Tag::copy).toList();
                var packet = new SetNbtSidebarS2CPacket(name, newNBT);
                for(var player : manager.server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, packet);
                }
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(nbtProvider, path);
        }
    }

    public interface SidebarNBTProvider {
        Map<String, MapCodec<? extends SidebarNBTProvider>> IMPLEMENTATION_CODECS = ImmutableMap.<String, MapCodec<? extends SidebarNBTProvider>>builder()
                .put(ENTITY_DATA_OBJECT_TYPE, EntitySidebarNbtProvider.CODEC)
                .put(BLOCK_DATA_OBJECT_TYPE, BlockSidebarNbtProvider.CODEC)
                .put(STORAGE_DATA_OBJECT_TYPE, StorageSidebarNbtProvider.CODEC)
                .build();

        MapCodec<SidebarNBTProvider> CODEC = Codec.STRING.dispatchMap("type", SidebarNBTProvider::getProviderTypeName, IMPLEMENTATION_CODECS::get);

        @Nullable
        CompoundTag getNbt(HolderLookup.Provider registryLookup, ServerNbtSidebarManager manager);
        boolean isDataObject(DataAccessor dataObject);
        String getDefaultNamePrefix();
        String getProviderTypeName();
    }

    public record EntitySidebarNbtProvider(UUID uuid) implements SidebarNBTProvider {
        public static final MapCodec<EntitySidebarNbtProvider> CODEC = UUIDUtil.AUTHLIB_CODEC.xmap(
                EntitySidebarNbtProvider::new,
                EntitySidebarNbtProvider::uuid
        ).fieldOf("uuid");

        @Override
        public CompoundTag getNbt(HolderLookup.Provider registryLookup, ServerNbtSidebarManager manager) {
            for (var world : manager.server.getAllLevels()) {
                var entity = world.getEntity(uuid);
                if (entity != null) {
                    return NbtPredicate.getEntityTagToCompare(entity);
                }
            }
            return null;
        }

        @Override
        public boolean isDataObject(DataAccessor dataObject) {
            return dataObject instanceof EntityDataAccessorAccessor entityDataObjectAccessor && entityDataObjectAccessor.getEntity().getUUID().equals(uuid);
        }

        @Override
        public String getDefaultNamePrefix() {
            return uuid.toString();
        }

        @Override
        public String getProviderTypeName() {
            return ENTITY_DATA_OBJECT_TYPE;
        }
    }

    public record BlockSidebarNbtProvider(BlockPos pos, ResourceKey<net.minecraft.world.level.Level> worldKey) implements SidebarNBTProvider {
        public static final MapCodec<BlockSidebarNbtProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(BlockSidebarNbtProvider::pos),
                Identifier.CODEC.xmap(id -> ResourceKey.create(Registries.DIMENSION, id), ResourceKey::identifier).fieldOf("world").forGetter(BlockSidebarNbtProvider::worldKey)
        ).apply(instance, BlockSidebarNbtProvider::new));

        @Override
        public CompoundTag getNbt(HolderLookup.Provider registryLookup, ServerNbtSidebarManager manager) {
            var world = manager.server.getLevel(worldKey);
            if(world == null) return null;
            var blockEntity = world.getChunkAt(pos).getBlockEntity(pos);
            return blockEntity == null ? null : blockEntity.saveWithoutMetadata(registryLookup);
        }

        @Override
        public boolean isDataObject(DataAccessor dataObject) {
            if(dataObject instanceof BlockDataAccessorAccessor blockDataObjectAccessor) {
                var pos = blockDataObjectAccessor.getPos();
                var worldKey = Objects.requireNonNull(blockDataObjectAccessor.getEntity().getLevel()).dimension();
                return pos.equals(this.pos) && worldKey.equals(this.worldKey);
            }
            return false;
        }

        @Override
        public String getDefaultNamePrefix() {
            return "[%d;%d;%d]-%s".formatted(pos.getX(), pos.getY(), pos.getZ(), worldKey.identifier().toString());
        }

        @Override
        public String getProviderTypeName() {
            return BLOCK_DATA_OBJECT_TYPE;
        }
    }

    public record StorageSidebarNbtProvider(Identifier id) implements SidebarNBTProvider {
        private static final MapCodec<StorageSidebarNbtProvider> CODEC = Identifier.CODEC.xmap(
                StorageSidebarNbtProvider::new,
                StorageSidebarNbtProvider::id
        ).fieldOf("id");

        @Override
        public CompoundTag getNbt(HolderLookup.Provider registryLookup, ServerNbtSidebarManager manager) {
            try {
                return manager.server.getCommandStorage().get(id);
            } catch(NullPointerException e) {
                //Thrown when dataCommandStorage is null
                return null;
            }
        }

        @Override
        public boolean isDataObject(DataAccessor dataObject) {
            return dataObject instanceof StorageDataAccessorAccessor storageDataObjectAccessor && storageDataObjectAccessor.getId().equals(id);
        }

        @Override
        public String getDefaultNamePrefix() {
            return id.toString();
        }

        @Override
        public String getProviderTypeName() {
            return STORAGE_DATA_OBJECT_TYPE;
        }
    }
}
