package com.miyazaki.hwmz;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraft.world.InteractionResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = HWmz.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LobbySessionManager {
    private static final BlockPos LOBBY_ORIGIN = new BlockPos(0, 200, 0);
    private static final BlockPos START_BUTTON = LOBBY_ORIGIN.offset(0, 2, -5);
    private static final double LOBBY_X = 0.5D;
    private static final double LOBBY_Y = 200.0D;
    private static final double LOBBY_Z = 0.5D;

    private static final Set<UUID> participants = new HashSet<>();
    private static final Set<UUID> alivePlayers = new HashSet<>();

    private static boolean sessionActive;
    private static double sessionX;
    private static double sessionZ;
    private static double previousBorderX;
    private static double previousBorderZ;
    private static double previousBorderSize = 5.9999968E7D;

    private LobbySessionManager() {
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

        if (sessionActive) {
            participants.add(player.getUUID());
            alivePlayers.add(player.getUUID());
            teleportToSession(player, server.overworld());
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
        ServerPlayer player = event.getEntity();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ServerLevel overworld = server.overworld();
        if (sessionActive && participants.contains(player.getUUID())) {
            alivePlayers.add(player.getUUID());
            teleportToSession(player, overworld);
        } else {
            teleportToLobby(player, overworld);
        }
    }

    @SubscribeEvent
    public static void onLobbyButton(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }

        BlockHitResult hit = event.getHitVec();
        if (!hit.getBlockPos().equals(START_BUTTON)) {
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
        // The lobby is a 10x10 footprint with a four-block-high enclosed interior.
        for (int x = -5; x < 5; x++) {
            for (int z = -5; z < 5; z++) {
                level.setBlock(new BlockPos(x, 199, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 204, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);

                for (int y = 200; y < 204; y++) {
                    boolean wall = x == -5 || x == 4 || z == -5 || z == 4;
                    if (wall) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        BlockState button = Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACING, Direction.SOUTH)
                .setValue(ButtonBlock.FACE, AttachFace.WALL);
        level.setBlock(START_BUTTON, button, 3);
    }

    private static void startSession(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ensureLobby(level);

        WorldSpawn spawn = findRandomSurfaceSpawn(level);
        if (spawn == null) {
            return;
        }

        var border = level.getWorldBorder();
        previousBorderX = border.getCenterX();
        previousBorderZ = border.getCenterZ();
        previousBorderSize = border.getSize();

        sessionX = spawn.x();
        sessionZ = spawn.z();
        border.setCenter(sessionX, sessionZ);
        border.setSize(1000.0D);

        participants.clear();
        alivePlayers.clear();

        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            participants.add(onlinePlayer.getUUID());
            alivePlayers.add(onlinePlayer.getUUID());
            onlinePlayer.teleportTo(level, spawn.x(), spawn.y(), spawn.z(),
                    Set.of(), onlinePlayer.getYRot(), onlinePlayer.getXRot());
        }

        sessionActive = !participants.isEmpty();
    }

    private static void endSession(MinecraftServer server) {
        if (!sessionActive || server == null) {
            return;
        }

        sessionActive = false;
        ServerLevel level = server.overworld();

        var border = level.getWorldBorder();
        border.setCenter(previousBorderX, previousBorderZ);
        border.setSize(previousBorderSize);

        participants.clear();
        alivePlayers.clear();

        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            teleportToLobby(onlinePlayer, level);
        }
    }

    private static WorldSpawn findRandomSurfaceSpawn(ServerLevel level) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < 100; attempt++) {
            int x = random.nextInt(-30000, 30001);
            int z = random.nextInt(-30000, 30001);

            // Force terrain generation before querying the surface height.
            level.getChunk(x >> 4, z >> 4);

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos ground = feet.below();

            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) {
                continue;
            }

            if (!level.getBiome(feet).is(Biomes.FOREST) && !level.getBiome(feet).is(Biomes.PLAINS)) {
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

        return null;
    }

    private static void teleportToLobby(ServerPlayer player, ServerLevel overworld) {
        player.teleportTo(overworld, LOBBY_X, LOBBY_Y, LOBBY_Z,
                Set.of(), player.getYRot(), player.getXRot());
    }

    private static void teleportToSession(ServerPlayer player, ServerLevel overworld) {
        int x = (int) Math.floor(sessionX);
        int z = (int) Math.floor(sessionZ);
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        player.teleportTo(overworld, sessionX, y, sessionZ,
                Set.of(), player.getYRot(), player.getXRot());
    }

    private record WorldSpawn(double x, double y, double z) {
    }
}
