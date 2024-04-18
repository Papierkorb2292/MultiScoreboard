package net.papierkorb2292.multiscoreboard;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.argument.NbtPathArgumentType;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
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

    static {
        try {
            ROOT_PATH = new NbtPathArgumentType().parse(new StringReader(""));
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final DynamicCommandExceptionType INVALID_DATA_SOURCE_EXCEPTION = new DynamicCommandExceptionType(arg -> Text.of("Unknown data source: " + arg));

    private final MinecraftServer server;
    private final Map<String, Entry> entries = new HashMap<>();

    public ServerNbtSidebarManager(MinecraftServer server) {
        this.server = server;
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
            sidebarNBTProvider = new EntitySidebarNbtProvider(uuid, this);
        } else if(dataObject instanceof BlockDataObjectAccessor blockDataObjectAccessor) {
            var pos = blockDataObjectAccessor.getPos();
            var worldKey = Objects.requireNonNull(blockDataObjectAccessor.getBlockEntity().getWorld()).getRegistryKey();
            sidebarNBTProvider = new BlockSidebarNbtProvider(pos, worldKey, this);
        } else if(dataObject instanceof StorageDataObjectAccessor storageDataObjectAccessor) {
            var id = storageDataObjectAccessor.getId();
            sidebarNBTProvider = new StorageSidebarNbtProvider(id, this);
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
        entry.updateNbt(name);
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

    public void tick() {
        for(var entry : entries.entrySet()) {
            entry.getValue().updateNbt(entry.getKey());
        }
    }

    public void onPlayerJoin(PacketSender packetSender) {
        for(var entry : entries.entrySet()) {
            var packet = new SetNbtSidebarS2CPacket(entry.getKey(), entry.getValue().getLastSentNbt());
            packetSender.sendPacket(packet);
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        var entriesNbt = new NbtCompound();
        for(var entry : entries.entrySet()) {
            var name = entry.getKey();
            var dataSource = entry.getValue();
            var dataSourceNbt = new NbtCompound();
            var nbtProviderNbt = new NbtCompound();
            dataSource.nbtProvider.writeProviderDataToNbt(nbtProviderNbt);
            if(dataSource.nbtProvider instanceof EntitySidebarNbtProvider) {
                nbtProviderNbt.putString("type", ENTITY_DATA_OBJECT_TYPE);
            } else if(dataSource.nbtProvider instanceof BlockSidebarNbtProvider) {
                nbtProviderNbt.putString("type", BLOCK_DATA_OBJECT_TYPE);
            } else if(dataSource.nbtProvider instanceof StorageSidebarNbtProvider) {
                nbtProviderNbt.putString("type", STORAGE_DATA_OBJECT_TYPE);
            } else {
                MultiScoreboard.LOGGER.error("Error saving NBT Sidebar '{}', unknown dataSource nbt provider: '{}'", name, dataSource.nbtProvider.getClass().getName());
                continue;
            }
            dataSourceNbt.put("nbtProvider", nbtProviderNbt);
            dataSourceNbt.putString("path", dataSource.path.toString());
            entriesNbt.put(name, dataSourceNbt);
        };
        nbt.put("entries", entriesNbt);
        return nbt;
    }

    public ServerNbtSidebarManager readNbt(NbtCompound nbt) {
        var entriesNbt = nbt.getCompound("entries");
        if(!(entriesNbt instanceof NbtCompound)) return this;
        for(var name : entriesNbt.getKeys()) {
            try {
                var dataSourceNbt = entriesNbt.getCompound(name);
                if(!dataSourceNbt.contains("path", NbtElement.STRING_TYPE) || !dataSourceNbt.contains("nbtProvider", NbtElement.COMPOUND_TYPE)) {
                    MultiScoreboard.LOGGER.error("Error loading NBT Sidebar '{}', could not load data source, because it is missing tags", name);
                    continue;
                }
                NbtPathArgumentType.NbtPath path;
                path = NbtPathArgumentType.nbtPath().parse(new StringReader(dataSourceNbt.getString("path")));
                var nbtProviderNbt = dataSourceNbt.getCompound("nbtProvider");
                if(!nbtProviderNbt.contains("type", NbtElement.STRING_TYPE)) {
                    MultiScoreboard.LOGGER.error("Error loading NBT Sidebar '{}', could not load data source, because 'dataObject' is missing the 'type' tag", name);
                    continue;
                }
                SidebarNBTProvider sidebarNBTProvider;
                switch(nbtProviderNbt.getString("type")) {
                    case ENTITY_DATA_OBJECT_TYPE:
                        sidebarNBTProvider = EntitySidebarNbtProvider.fromNbt(nbtProviderNbt, this);
                        break;
                    case BLOCK_DATA_OBJECT_TYPE:
                        sidebarNBTProvider = BlockSidebarNbtProvider.fromNbt(nbtProviderNbt, this);
                        break;
                    case STORAGE_DATA_OBJECT_TYPE:
                        sidebarNBTProvider = StorageSidebarNbtProvider.fromNbt(nbtProviderNbt, this);
                        break;
                    default:
                        MultiScoreboard.LOGGER.error("Error loading NBT Sidebar '{}', unknown nbt provider type: '{}'", name, nbtProviderNbt.getString("type"));
                        continue;
                }
                addEntry(name, sidebarNBTProvider, path);
            } catch(Exception e) {
                MultiScoreboard.LOGGER.error("Error loading NBT Sidebar '{}': ", name, e);
            }
        }
        return this;
    }

    public static PersistentState.Type<ServerNbtSidebarManager> getPersistentStateType(MinecraftServer server) {
        return new PersistentState.Type<>(
                () -> new ServerNbtSidebarManager(server),
                (nbt, decoderRegistryLookup) -> new ServerNbtSidebarManager(server).readNbt(nbt),
                null
        );
    }

    public final class Entry {
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

        private void updateNbt(String name) {
            List<NbtElement> newNBT;
            try {
                var fullNbt = nbtProvider.getNbt(server.getRegistryManager());
                newNBT = fullNbt == null ? Collections.emptyList() : path.get(fullNbt);
            } catch(CommandSyntaxException e) {
                newNBT = Collections.emptyList();
            }
            if(!newNBT.equals(lastSentNbt)) {
                lastSentNbt = newNBT.stream().map(NbtElement::copy).toList();
                var packet = new SetNbtSidebarS2CPacket(name, newNBT);
                for(var player : server.getPlayerManager().getPlayerList()) {
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
        @Nullable
        NbtCompound getNbt(RegistryWrapper.WrapperLookup registryLookup);
        void writeProviderDataToNbt(NbtCompound nbt);
        boolean isDataObject(DataCommandObject dataObject);
        String getDefaultNamePrefix();
    }

    public static class EntitySidebarNbtProvider implements SidebarNBTProvider {
        private final UUID uuid;
        private final ServerNbtSidebarManager manager;

        public EntitySidebarNbtProvider(UUID uuid, ServerNbtSidebarManager manager) {
            this.uuid = uuid;
            this.manager = manager;
        }

        @Override
        public NbtCompound getNbt(RegistryWrapper.WrapperLookup registryLookup) {
            for (var world : manager.server.getWorlds()) {
                var entity = world.getEntity(uuid);
                if (entity != null) {
                    return entity.writeNbt(new NbtCompound());
                }
            }
            return null;
        }

        @Override
        public void writeProviderDataToNbt(NbtCompound nbt) {
            nbt.putUuid("uuid", uuid);
        }

        @Override
        public boolean isDataObject(DataCommandObject dataObject) {
            return dataObject instanceof EntityDataObjectAccessor entityDataObjectAccessor && entityDataObjectAccessor.getEntity().getUuid().equals(uuid);
        }

        @Override
        public String getDefaultNamePrefix() {
            return uuid.toString();
        }

        public static EntitySidebarNbtProvider fromNbt(NbtCompound nbt, ServerNbtSidebarManager manager) {
            return new EntitySidebarNbtProvider(nbt.getUuid("uuid"), manager);
        }
    }

    public static class BlockSidebarNbtProvider implements SidebarNBTProvider {
        private final BlockPos pos;
        private final RegistryKey<net.minecraft.world.World> worldKey;
        private final ServerNbtSidebarManager manager;

        public BlockSidebarNbtProvider(BlockPos pos, RegistryKey<net.minecraft.world.World> worldKey, ServerNbtSidebarManager manager) {
            this.pos = pos;
            this.worldKey = worldKey;
            this.manager = manager;
        }

        @Override
        public NbtCompound getNbt(RegistryWrapper.WrapperLookup registryLookup) {
            var world = manager.server.getWorld(worldKey);
            if(world == null) return null;
            var blockEntity = world.getBlockEntity(pos);
            return blockEntity == null ? null : blockEntity.createNbt(registryLookup);
        }

        @Override
        public void writeProviderDataToNbt(NbtCompound nbt) {
            nbt.put("pos", BlockPos.CODEC.encode(pos, NbtOps.INSTANCE, NbtEnd.INSTANCE).getOrThrow());
            nbt.putString("world", worldKey.getValue().toString());
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

        public static BlockSidebarNbtProvider fromNbt(NbtCompound nbt, ServerNbtSidebarManager manager) {
            var pos = BlockPos.CODEC.parse(NbtOps.INSTANCE, nbt.get("pos")).getOrThrow();
            var worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(nbt.getString("world")));
            return new BlockSidebarNbtProvider(pos, worldKey, manager);
        }
    }

    public static class StorageSidebarNbtProvider implements SidebarNBTProvider {
        private final Identifier id;
        private final ServerNbtSidebarManager manager;

        public StorageSidebarNbtProvider(Identifier id, ServerNbtSidebarManager manager) {
            this.id = id;
            this.manager = manager;
        }

        @Override
        public NbtCompound getNbt(RegistryWrapper.WrapperLookup registryLookup) {
            try {
                return manager.server.getDataCommandStorage().get(id);
            } catch(NullPointerException e) {
                //Thrown when dataCommandStorage is null
                return null;
            }
        }

        @Override
        public void writeProviderDataToNbt(NbtCompound nbt) {
            nbt.putString("id", id.toString());
        }

        @Override
        public boolean isDataObject(DataCommandObject dataObject) {
            return dataObject instanceof StorageDataObjectAccessor storageDataObjectAccessor && storageDataObjectAccessor.getId().equals(id);
        }

        @Override
        public String getDefaultNamePrefix() {
            return id.toString();
        }

        public static StorageSidebarNbtProvider fromNbt(NbtCompound nbt, ServerNbtSidebarManager manager) {
            return new StorageSidebarNbtProvider(new Identifier(nbt.getString("id")), manager);
        }
    }
}
