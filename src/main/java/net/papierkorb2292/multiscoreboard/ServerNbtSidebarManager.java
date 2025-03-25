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
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.papierkorb2292.multiscoreboard.mixin.BlockDataObjectAccessor;
import net.papierkorb2292.multiscoreboard.mixin.EntityDataObjectAccessor;
import net.papierkorb2292.multiscoreboard.mixin.StorageDataObjectAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServerNbtSidebarManager extends PersistentState {

    private static final String ENTITY_DATA_OBJECT_TYPE = "entity";
    private static final String BLOCK_DATA_OBJECT_TYPE = "block";
    private static final String STORAGE_DATA_OBJECT_TYPE = "storage";

    public static final NbtPathArgumentType.NbtPath ROOT_PATH;

    private static final Codec<Map<String, Entry>> CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC);

    static {
        try {
            ROOT_PATH = new NbtPathArgumentType().parse(new StringReader(""));
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final DynamicCommandExceptionType INVALID_DATA_SOURCE_EXCEPTION = new DynamicCommandExceptionType(arg -> Text.of("Unknown data source: " + arg));

    private final MinecraftServer server;
    private final Map<String, Entry> entries;

    public ServerNbtSidebarManager(MinecraftServer server) {
        this(server, new HashMap<>());
    }

    public ServerNbtSidebarManager(MinecraftServer server, Map<String, Entry> entries) {
        this.server = server;
        this.entries = new HashMap<>(entries);
    }

    public void tick() {
        for(var entry : entries.entrySet()) {
            entry.getValue().updateNbt(entry.getKey(), this);
        }
    }

    /**
     * Adds a new NBT sidebar.
     * @param name The name of the sidebar. If null, a default name will be used consisting of the dataObject and path
     * @param dataObject The data source object to get the NBT data from.
     * @param path The path to the NBT data.
     * @return The name of the added sidebar.
     * @throws CommandSyntaxException If the data source object is not a valid data source.
     */
    public String addEntry(@Nullable String name, DataCommandObject dataObject, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
        SidebarNBTProvider sidebarNBTProvider;
        if(dataObject instanceof EntityDataObjectAccessor entityDataObjectAccessor) {
            var uuid = entityDataObjectAccessor.getEntity().getUuid();
            sidebarNBTProvider = new EntitySidebarNbtProvider(uuid);
        } else if(dataObject instanceof BlockDataObjectAccessor blockDataObjectAccessor) {
            var pos = blockDataObjectAccessor.getPos();
            var worldKey = Objects.requireNonNull(blockDataObjectAccessor.getBlockEntity().getWorld()).getRegistryKey();
            sidebarNBTProvider = new BlockSidebarNbtProvider(pos, worldKey);
        } else if(dataObject instanceof StorageDataObjectAccessor storageDataObjectAccessor) {
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

    private void addEntry(String name, SidebarNBTProvider sidebarNBTProvider, NbtPathArgumentType.NbtPath path) {
        markDirty();
        var entry = new Entry(sidebarNBTProvider, path);
        entries.put(name, entry);
        entry.updateNbt(name, this);
    }

    public boolean removeEntry(String name) {
        markDirty();
        var removedPacket = new RemoveNbtSidebarS2CPacket(name);
        for(var player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, removedPacket);
        }
        return entries.remove(name) != null;
    }

    /**
     * Removes all entries that take their dataSource from given dataSource object.
     * @param dataObject The dataSource object to remove entries for.
     * @return The number of removed entries.
     */
    public int removeEntriesOfDataObject(DataCommandObject dataObject) {
        var entryCount = entries.size();
        entries.entrySet().removeIf(entry -> {
            var remove = entry.getValue().nbtProvider.isDataObject(dataObject);
            if(remove) {
                var removedPacket = new RemoveNbtSidebarS2CPacket(entry.getKey());
                for(var player : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(player, removedPacket);
                }
            }
            return remove;
        });
        if(entryCount != entries.size()) markDirty();
        return entryCount - entries.size();
    }

    public int removeAllEntries() {
        var count = entries.size();
        for(var name : entries.keySet()) {
            var removedPacket = new RemoveNbtSidebarS2CPacket(name);
            for(var player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, removedPacket);
            }
        }
        entries.clear();
        markDirty();
        return count;
    }

    public String getEntryNameIfMatches(String name, DataCommandObject dataObject, NbtPathArgumentType.NbtPath path) {
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

    public static PersistentStateType<ServerNbtSidebarManager> getPersistentStateType(MinecraftServer server) {
        return new PersistentStateType<>(
                "multiscoreboard_nbt",
                () -> new ServerNbtSidebarManager(server),
                CODEC.xmap(entries -> new ServerNbtSidebarManager(server, entries), manager -> manager.entries),
                null
        );
    }

    public static final class Entry {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SidebarNBTProvider.CODEC.fieldOf("nbtProvider").forGetter(Entry::getNbtProvider),
                Codecs.exceptionCatching(Codec.STRING.flatXmap(
                        string -> {
                            try {
                                return DataResult.success(NbtPathArgumentType.nbtPath().parse(new StringReader(string)));
                            } catch(CommandSyntaxException e) {
                                return DataResult.error(e::getMessage);
                            }
                        },
                        path -> DataResult.success(path.toString())
                )).fieldOf("path").forGetter(Entry::getPath)
        ).apply(instance, Entry::new));

        private final SidebarNBTProvider nbtProvider;
        private final NbtPathArgumentType.NbtPath path;
        private List<NbtElement> lastSentNbt = null;

        public Entry(SidebarNBTProvider nbtProvider, NbtPathArgumentType.NbtPath path) {
            this.nbtProvider = nbtProvider;
            this.path = path;
        }

        public SidebarNBTProvider getNbtProvider() {
            return nbtProvider;
        }

        public NbtPathArgumentType.NbtPath getPath() {
            return path;
        }

        public List<NbtElement> getLastSentNbt() {
            return lastSentNbt;
        }

        private void updateNbt(String name, ServerNbtSidebarManager manager) {
            List<NbtElement> newNBT;
            try {
                var fullNbt = nbtProvider.getNbt(manager.server.getRegistryManager(), manager);
                newNBT = fullNbt == null ? Collections.emptyList() : path.get(fullNbt);
            } catch(CommandSyntaxException e) {
                newNBT = Collections.emptyList();
            }
            if(!newNBT.equals(lastSentNbt)) {
                lastSentNbt = newNBT.stream().map(NbtElement::copy).toList();
                var packet = new SetNbtSidebarS2CPacket(name, newNBT);
                for(var player : manager.server.getPlayerManager().getPlayerList()) {
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
        NbtCompound getNbt(RegistryWrapper.WrapperLookup registryLookup, ServerNbtSidebarManager manager);
        boolean isDataObject(DataCommandObject dataObject);
        String getDefaultNamePrefix();
        String getProviderTypeName();
    }

    public record EntitySidebarNbtProvider(UUID uuid) implements SidebarNBTProvider {
        public static final MapCodec<EntitySidebarNbtProvider> CODEC = Uuids.CODEC.xmap(
                EntitySidebarNbtProvider::new,
                EntitySidebarNbtProvider::uuid
        ).fieldOf("uuid");

        @Override
        public NbtCompound getNbt(RegistryWrapper.WrapperLookup registryLookup, ServerNbtSidebarManager manager) {
            for (var world : manager.server.getWorlds()) {
                var entity = world.getEntity(uuid);
                if (entity != null) {
                    final var nbt = entity.writeNbt(new NbtCompound());
                    if (entity instanceof PlayerEntity player) {
                        ItemStack itemStack = player.getInventory().getSelectedStack();
                        if (!itemStack.isEmpty()) {
                            nbt.put("SelectedItem", itemStack.toNbt(entity.getRegistryManager()));
                        }
                    }
                    return nbt;
                }
            }
            return null;
        }

        @Override
        public boolean isDataObject(DataCommandObject dataObject) {
            return dataObject instanceof EntityDataObjectAccessor entityDataObjectAccessor && entityDataObjectAccessor.getEntity().getUuid().equals(uuid);
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

    public record BlockSidebarNbtProvider(BlockPos pos, RegistryKey<net.minecraft.world.World> worldKey) implements SidebarNBTProvider {
        public static final MapCodec<BlockSidebarNbtProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(BlockSidebarNbtProvider::pos),
                Identifier.CODEC.xmap(id -> RegistryKey.of(RegistryKeys.WORLD, id), RegistryKey::getValue).fieldOf("world").forGetter(BlockSidebarNbtProvider::worldKey)
        ).apply(instance, BlockSidebarNbtProvider::new));

        @Override
        public NbtCompound getNbt(RegistryWrapper.WrapperLookup registryLookup, ServerNbtSidebarManager manager) {
            var world = manager.server.getWorld(worldKey);
            if(world == null) return null;
            var blockEntity = world.getWorldChunk(pos).getBlockEntity(pos);
            return blockEntity == null ? null : blockEntity.createNbt(registryLookup);
        }

        @Override
        public boolean isDataObject(DataCommandObject dataObject) {
            if(dataObject instanceof BlockDataObjectAccessor blockDataObjectAccessor) {
                var pos = blockDataObjectAccessor.getPos();
                var worldKey = Objects.requireNonNull(blockDataObjectAccessor.getBlockEntity().getWorld()).getRegistryKey();
                return pos.equals(this.pos) && worldKey.equals(this.worldKey);
            }
            return false;
        }

        @Override
        public String getDefaultNamePrefix() {
            return "[%d;%d;%d]-%s".formatted(pos.getX(), pos.getY(), pos.getZ(), worldKey.getValue().toString());
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
        public NbtCompound getNbt(RegistryWrapper.WrapperLookup registryLookup, ServerNbtSidebarManager manager) {
            try {
                return manager.server.getDataCommandStorage().get(id);
            } catch(NullPointerException e) {
                //Thrown when dataCommandStorage is null
                return null;
            }
        }

        @Override
        public boolean isDataObject(DataCommandObject dataObject) {
            return dataObject instanceof StorageDataObjectAccessor storageDataObjectAccessor && storageDataObjectAccessor.getId().equals(id);
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
