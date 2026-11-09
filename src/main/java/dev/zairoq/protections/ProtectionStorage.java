package dev.zairoq.protections;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
public final class ProtectionStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CLAIM_LIST = new TypeToken<List<Claim>>() {}.getType();
    private static final List<Claim> CLAIMS = new ArrayList<>();
    private static MinecraftServer activeServer;
    private static boolean storageHealthy = true;
    private ProtectionStorage() {}
    public static void registerLifecycle() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> { activeServer = server; load(server); });
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> { if (server == activeServer) save(server); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (server == activeServer) { save(server); CLAIMS.clear(); activeServer = null; storageHealthy = true; }
        });
    }
    private static Path dataFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("zairoq_protections.json");
    }
    private static void load(MinecraftServer server) {
        CLAIMS.clear(); storageHealthy = true;
        Path file = dataFile(server);
        if (!Files.exists(file)) return;
        try {
            List<Claim> loaded = GSON.fromJson(Files.readString(file), CLAIM_LIST);
            if (loaded != null) for (Claim claim : loaded) {
                if (claim != null && claim.id != null && claim.owner != null && claim.dimension != null
                        && claim.size > 0 && claim.size <= 100) {
                    if (claim.coOwners == null) claim.coOwners = new LinkedHashMap<>();
                    if (claim.trusted == null) claim.trusted = new LinkedHashMap<>();
                    CLAIMS.add(claim);
                }
            }
            ZairoqProtections.LOGGER.info("Loaded {} land claims.", CLAIMS.size());
        } catch (Exception exception) {
            storageHealthy = false;
            ZairoqProtections.LOGGER.error("Could not read claims file {}. Writes are disabled to preserve the original file.", file, exception);
        }
    }
    public static void save(MinecraftServer server) {
        if (!storageHealthy) {
            ZairoqProtections.LOGGER.error("Skipping claim save because the existing claims file failed to load; refusing to overwrite it.");
            return;
        }
        Path file = dataFile(server);
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(CLAIMS, CLAIM_LIST));
            try {
                Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) { ZairoqProtections.LOGGER.error("Could not save land claims to {}.", file, exception); }
    }
    public static List<Claim> all() { return List.copyOf(CLAIMS); }
    public static Claim find(String dimension, BlockPos pos) {
        for (Claim claim : CLAIMS) if (claim.dimension.equals(dimension) && claim.contains(pos.getX(), pos.getZ())) return claim;
        return null;
    }
    public static Claim findById(String id) { for (Claim claim : CLAIMS) if (claim.id.equals(id)) return claim; return null; }
    public static boolean canBuild(Claim claim, UUID player) {
        return claim.owner.equals(player.toString()) || claim.coOwners.containsKey(player.toString());
    }
    public static boolean canInteract(Claim claim, UUID player) {
        String id = player.toString();
        return claim.owner.equals(id) || claim.coOwners.containsKey(id) || claim.trusted.containsKey(id);
    }
    public static boolean overlaps(String dimension, int centerX, int centerZ, int size) {
        int minX = centerX - size / 2, minZ = centerZ - size / 2;
        int maxX = minX + size - 1, maxZ = minZ + size - 1;
        for (Claim claim : CLAIMS) if (claim.dimension.equals(dimension) && minX <= claim.maxX()
                && maxX >= claim.minX() && minZ <= claim.maxZ() && maxZ >= claim.minZ()) return true;
        return false;
    }
    public static Claim createProtectionStone(MinecraftServer server, ServerPlayer owner, Level level,
                                              BlockPos corePos, int size, String mineral) {
        String dimension = level.dimension().identifier().toString();
        if (!storageHealthy || overlaps(dimension, corePos.getX(), corePos.getZ(), size)) return null;
        Claim claim = new Claim();
        claim.id = UUID.randomUUID().toString().substring(0, 8);
        claim.owner = owner.getUUID().toString(); claim.ownerName = owner.getGameProfile().name();
        claim.dimension = dimension; claim.centerX = corePos.getX(); claim.centerZ = corePos.getZ(); claim.size = size;
        claim.coreX = corePos.getX(); claim.coreY = corePos.getY(); claim.coreZ = corePos.getZ();
        // Equal core/base coordinates mark a claim created by a crafted Protection Stone.
        claim.baseX = corePos.getX(); claim.baseY = corePos.getY(); claim.baseZ = corePos.getZ();
        claim.mineral = mineral;
        CLAIMS.add(claim); save(server); return claim;
    }
    /** Kept for compatibility with claims created by the former mineral-block + Lodestone system. */
    public static Claim create(MinecraftServer server, ServerPlayer owner, Level level,
                               BlockPos basePos, BlockPos lodestonePos, int size, String mineral) {
        String dimension = level.dimension().identifier().toString();
        if (!storageHealthy || overlaps(dimension, lodestonePos.getX(), lodestonePos.getZ(), size)) return null;
        Claim claim = new Claim();
        claim.id = UUID.randomUUID().toString().substring(0, 8);
        claim.owner = owner.getUUID().toString(); claim.ownerName = owner.getGameProfile().name();
        claim.dimension = dimension; claim.centerX = lodestonePos.getX(); claim.centerZ = lodestonePos.getZ(); claim.size = size;
        claim.coreX = lodestonePos.getX(); claim.coreY = lodestonePos.getY(); claim.coreZ = lodestonePos.getZ();
        claim.baseX = basePos.getX(); claim.baseY = basePos.getY(); claim.baseZ = basePos.getZ(); claim.mineral = mineral;
        CLAIMS.add(claim); save(server); return claim;
    }
    public static boolean remove(MinecraftServer server, Claim claim) {
        boolean removed = CLAIMS.remove(claim); if (removed) save(server); return removed;
    }
    public static boolean isProtected(Level level, BlockPos pos) {
        return find(level.dimension().identifier().toString(), pos) != null;
    }
    public static int claimSizeForBase(net.minecraft.world.level.block.state.BlockState state) {
        if (state.is(Blocks.COAL_BLOCK)) return 10;
        if (state.is(Blocks.COPPER_BLOCK.weathering().unaffected())) return 20;
        if (state.is(Blocks.DIAMOND_BLOCK)) return 50;
        if (state.is(Blocks.NETHERITE_BLOCK)) return 100;
        return 0;
    }
    public static String mineralName(net.minecraft.world.level.block.state.BlockState state) {
        if (state.is(Blocks.COAL_BLOCK)) return "coal";
        if (state.is(Blocks.COPPER_BLOCK.weathering().unaffected())) return "copper";
        if (state.is(Blocks.DIAMOND_BLOCK)) return "diamond";
        if (state.is(Blocks.NETHERITE_BLOCK)) return "netherite";
        return "unknown";
    }
    public static final class Claim {
        public String id, owner, ownerName, dimension, mineral;
        public int centerX, centerZ, size, coreX, coreY, coreZ, baseX, baseY, baseZ;
        public Map<String, String> coOwners = new LinkedHashMap<>();
        public Map<String, String> trusted = new LinkedHashMap<>();
        public int minX() { return centerX - size / 2; }
        public int minZ() { return centerZ - size / 2; }
        public int maxX() { return minX() + size - 1; }
        public int maxZ() { return minZ() + size - 1; }
        public boolean contains(int x, int z) { return x >= minX() && x <= maxX() && z >= minZ() && z <= maxZ(); }
        public boolean isCore(BlockPos pos) {
            return (pos.getX() == coreX && pos.getY() == coreY && pos.getZ() == coreZ)
                || (pos.getX() == baseX && pos.getY() == baseY && pos.getZ() == baseZ);
        }
    }
}
