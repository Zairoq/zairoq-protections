package dev.zairoq.protections;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class ZairoqProtections implements ModInitializer {
    public static final String MOD_ID = "zairoq_protections";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override public void onInitialize() {
        ProtectionStorage.registerLifecycle();
        ProtectionEvents.register();
        CommandRegistrationCallback.EVENT.register(ProtectionCommands::register);
        LOGGER.info("Zairoq Protections initialized.");
    }
}
