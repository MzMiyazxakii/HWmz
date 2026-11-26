package com.miyazaki.hwmz;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.StructureTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = HWmz.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LobbySessionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockPos LOBBY_ORIGIN = new BlockPos(0, 200, 0);
    private static final BlockPos START_BUTTON = LOBBY_ORIGIN.offset(0, 2, -5);
    private static final double LOBBY_X = 0.5D;
    private static final double LOBBY_Y = 200.0D;
    private static final double LOBBY_Z = 0.5D;
    private static final double GAME_BORDER_SIZE = 700.0D;

    private static final Set<UUID> participants = new HashSet<>();
    private static final Set<UUID> alivePlayers = new HashSet<>();

    private static boolean sessionActive;
    private static ServerLevel sessionLevel;
    private static ResourceKey<Level> sessionDimensionKey;
    private static ChunkProgressListener sessionProgressListener;
    private static double sessionX;
    private static double sessionY;
    private static double sessionZ;

    private LobbySessionManager() {
    }

    public static boolean isLobbyPlayer(net.minecraft.world.entity.player.Player player) {
        if (player == null || player.level().dimension() != Level.OVERWORLD) return false;
        return player.getX() >= -6 && player.getX() <= 5
                && player.getZ() >= -6 && player.getZ() <= 5
                && player.getY() >= 198 && player.getY() <= 206;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ensureLobby(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ensureLobby(server.overworld());

        if (sessionActive && sessionLevel != null) {
            participants.add(player.getUUID());
            alivePlayers.add(player.getUUID());
            teleportToSession(player);
        } else {
            teleportToLobby(player, server.overworld());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !sessionActive) {
            return;
        }

        participants.remove(player.getUUID());
        alivePlayers.remove(player.getUUID());

        if (alivePlayers.isEmpty()) {
            endSession(player.getServer());
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !sessionActive) {
            return;
        }

        alivePlayers.remove(player.getUUID());
        if (alivePlayers.isEmpty()) {
            endSession(player.getServer());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        if (sessionActive && sessionLevel != null && participants.contains(player.getUUID())) {
            alivePlayers.add(player.getUUID());
            teleportToSession(player);
        } else {
            teleportToLobby(player, server.overworld());
        }
    }

    @SubscribeEvent
    public static void onLobbyButton(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }

        if (!event.getHitVec().getBlockPos().equals(START_BUTTON)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        MinecraftServer server = player.getServer();
        if (server != null && !sessionActive) {
            startSession(server);
        }
    }

    private static void ensureLobby(ServerLevel level) {
        for (int x = -5; x < 5; x++) {
            for (int z = -5; z < 5; z++) {
                level.setBlock(new BlockPos(x, 199, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 204, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);

                for (int y = 200; y < 204; y++) {
                    boolean wall = x == -5 || x == 4 || z == -5 || z == 4;
                    level.setBlock(new BlockPos(x, y, z),
                            wall ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        BlockState button = Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACING, net.minecraft.core.Direction.SOUTH)
                .setValue(ButtonBlock.FACE, AttachFace.WALL);
        level.setBlock(START_BUTTON, button, 3);
    }

    private static void startSession(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        ensureLobby(overworld);

        ServerLevel createdLevel = null;
        ResourceKey<Level> createdDimension = null;
        try {
            createdLevel = createFreshGameWorld(server);
            createdDimension = sessionDimensionKey;
            WorldSpawn spawn = findVillageSpawn(createdLevel);
            if (spawn == null) {
                closeAndDeleteGameWorld(server, createdLevel, createdDimension);
                sessionLevel = null;
                sessionDimensionKey = null;
                return;
            }

            sessionLevel = createdLevel;
            sessionX = spawn.x();
            sessionY = spawn.y();
            sessionZ = spawn.z();

            WorldBorder border = createdLevel.getWorldBorder();
            border.setCenter(spawn.x(), spawn.z());
            border.setSize(GAME_BORDER_SIZE);

            createdLevel.setDayTime(1000L);
            createdLevel.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            createdLevel.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            createdLevel.setWeatherParameters(0, 0, false, false);

            participants.clear();
            alivePlayers.clear();

            for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
                LobbyInventoryMenu.returnWarehouseItems(onlinePlayer);
                participants.add(onlinePlayer.getUUID());
                alivePlayers.add(onlinePlayer.getUUID());
                teleportToSession(onlinePlayer);
            }

            sessionActive = !participants.isEmpty();
            if (!sessionActive) {
                endSession(server);
            }
        } catch (Exception exception) {
            sessionActive = false;
            participants.clear();
            alivePlayers.clear();

            for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
                teleportToLobby(onlinePlayer, overworld);
            }

            if (createdLevel == null && sessionDimensionKey != null) {
                createdLevel = server.forgeGetWorldMap().get(sessionDimensionKey);
                createdDimension = sessionDimensionKey;
            }
            if (createdLevel != null && createdDimension != null) {
                try {
                    closeAndDeleteGameWorld(server, createdLevel, createdDimension);
                } catch (Exception cleanupException) {
                    LOGGER.error("Failed to clean up an incomplete HWMZ session world", cleanupException);
                }
            } else if (sessionProgressListener != null) {
                sessionProgressListener.stop();
                sessionProgressListener = null;
            }

            sessionLevel = null;
            sessionDimensionKey = null;
            LOGGER.error("Could not create a fresh HWMZ session world", exception);
        }
    }

    private static ServerLevel createFreshGameWorld(MinecraftServer server) throws Exception {
        long seed = WorldOptions.randomSeed();
        ResourceKey<Level> dimensionKey = ResourceKey.create(
                Registries.DIMENSION,
                new ResourceLocation(HWmz.MOD_ID, "session_" + UUID.randomUUID().toString().replace("-", ""))
        );
        sessionDimensionKey = dimensionKey;

        var registryAccess = server.registryAccess();
        var biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        // Vanilla villages only generate in village-compatible biomes; use plains for a guaranteed village.
        Holder<Biome> biome = biomeRegistry.getHolderOrThrow(Biomes.PLAINS);

        Holder<NoiseGeneratorSettings> noiseSettings = registryAccess
                .registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        ChunkGenerator generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), noiseSettings);

        LevelStem overworldStem = registryAccess.registryOrThrow(Registries.LEVEL_STEM)
                .getHolderOrThrow(LevelStem.OVERWORLD).value();
        LevelStem gameStem = new LevelStem(overworldStem.type(), generator);

        var worldData = server.getWorldData();
        WorldOptions worldOptions = new WorldOptions(
                seed,
                true,
                false
        );
        ServerLevelData levelData = new PrimaryLevelData(
                new LevelSettings(
                        "HWMZ Session",
                        worldData.getGameType(),
                        worldData.isHardcore(),
                        worldData.getDifficulty(),
                        worldData.getAllowCommands(),
                        worldData.getGameRules(),
                        worldData.getLevelSettings().getDataConfiguration()
                ),
                worldOptions,
                PrimaryLevelData.SpecialWorldProperty.NONE,
                Lifecycle.stable()
        );

        Executor executor = ObfuscationReflectionHelper.getPrivateValue(
                MinecraftServer.class, server, "f_129738_");
        LevelStorageSource.LevelStorageAccess storage = ObfuscationReflectionHelper.getPrivateValue(
                MinecraftServer.class, server, "f_129744_");
        if (executor == null || storage == null) {
            throw new IllegalStateException("Could not access the server world executor or save storage");
        }

        ChunkProgressListener progressListener = new StoringChunkProgressListener(11);
        progressListener.start();
        progressListener.updateSpawnPos(new ChunkPos(0, 0));
        sessionProgressListener = progressListener;

        ServerLevel newLevel = new ServerLevel(
                server,
                executor,
                storage,
                levelData,
                dimensionKey,
                gameStem,
                progressListener,
                false,
                BiomeManager.obfuscateSeed(seed),
                List.of(),
                true,
                null
        );

        Map<ResourceKey<Level>, ServerLevel> levels = server.forgeGetWorldMap();
        levels.put(dimensionKey, newLevel);
        server.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(newLevel));
        return newLevel;
    }

    private static WorldSpawn findVillageSpawn(ServerLevel level) {
        BlockPos village = level.findNearestMapStructure(
                StructureTags.VILLAGE,
                BlockPos.ZERO,
                64,
                false
        );
        if (village == null) {
            LOGGER.warn("No village structure found within 32 chunks of the session origin");
            return null;
        }

        int villageChunkX = village.getX() >> 4;
        int villageChunkZ = village.getZ() >> 4;

        // Generate the village and its surrounding chunks before selecting the player spawn.
        for (int chunkX = villageChunkX - 4; chunkX <= villageChunkX + 4; chunkX++) {
            for (int chunkZ = villageChunkZ - 4; chunkZ <= villageChunkZ + 4; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }

        WorldSpawn spawn = findSafeSpawnNear(level, village.getX(), village.getZ(), 80);
        if (spawn == null) {
            LOGGER.warn("A village was located, but no safe player spawn was found near it");
        }
        return spawn;
    }

    private static WorldSpawn findSafeSpawnNear(ServerLevel level, int centerX, int centerZ, int radius) {
        for (int distance = 0; distance <= radius; distance += 4) {
            for (int dx = -distance; dx <= distance; dx += 4) {
                for (int dz = -distance; dz <= distance; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != distance && distance != 0) {
                        continue;
                    }

                    int x = centerX + dx;
                    int z = centerZ + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos feet = new BlockPos(x, y, z);
                    BlockPos ground = feet.below();

                    if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) {
                        continue;
                    }

                    BlockState groundState = level.getBlockState(ground);
                    if (groundState.getFluidState().isEmpty()
                            && !groundState.getCollisionShape(level, ground).isEmpty()
                            && level.getBlockState(feet).isAir()
                            && level.getBlockState(feet.above()).isAir()
                            && level.getFluidState(feet).isEmpty()
                            && level.getFluidState(feet.above()).isEmpty()) {
                        return new WorldSpawn(x + 0.5D, y, z + 0.5D);
                    }
                }
            }
        }

        return null;
    }

    private static void endSession(MinecraftServer server) {
        if (!sessionActive || server == null) {
            return;
        }

        sessionActive = false;
        participants.clear();
        alivePlayers.clear();

        ServerLevel overworld = server.overworld();
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            teleportToLobby(onlinePlayer, overworld);
        }

        ServerLevel oldLevel = sessionLevel;
        ResourceKey<Level> oldDimensionKey = sessionDimensionKey;
        sessionLevel = null;
        sessionDimensionKey = null;

        if (oldLevel != null && oldDimensionKey != null) {
            closeAndDeleteGameWorld(server, oldLevel, oldDimensionKey);
        }
    }

    private static void closeAndDeleteGameWorld(
            MinecraftServer server,
            ServerLevel level,
            ResourceKey<Level> dimensionKey
    ) {
        if (sessionProgressListener != null) {
            sessionProgressListener.stop();
            sessionProgressListener = null;
        }
        server.forgeGetWorldMap().remove(dimensionKey);
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(level));
        try {
            level.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not close the temporary session world", exception);
        }
        server.markWorldsDirty();

        try {
            Path dimensionPath = getStorage(server).getDimensionPath(dimensionKey);
            if (Files.exists(dimensionPath)) {
                try (var paths = Files.walk(dimensionPath)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not delete the temporary session world files", exception);
        }
    }

    private static LevelStorageSource.LevelStorageAccess getStorage(MinecraftServer server) {
        LevelStorageSource.LevelStorageAccess storage = ObfuscationReflectionHelper.getPrivateValue(
                MinecraftServer.class, server, "f_129744_");
        if (storage == null) {
            throw new IllegalStateException("Server save storage is unavailable");
        }
        return storage;
    }

    private static void teleportToLobby(ServerPlayer player, ServerLevel overworld) {
        player.teleportTo(overworld, LOBBY_X, LOBBY_Y, LOBBY_Z,
                Set.of(), player.getYRot(), player.getXRot());
    }

    private static void teleportToSession(ServerPlayer player) {
        if (sessionLevel == null) {
            return;
        }
        player.teleportTo(sessionLevel, sessionX, sessionY, sessionZ,
                Set.of(), player.getYRot(), player.getXRot());
    }

    private record WorldSpawn(double x, double y, double z) {
    }
}
