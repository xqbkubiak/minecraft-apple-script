package pl.bkubiak.bkrynek.client.util;

import net.minecraft.client.MinecraftClient;
import java.util.Random;

public class AfkController {
    private static boolean enabled = false;
    private static long nextJumpTime = 0;
    private static int jumpTicksRemaining = 0;
    private static final Random random = new Random();
    private static boolean wasScreenOpen = false;
    private static long reopenTime = 0;
    
    // Jump interval: 2 min to 2:30 min (in milliseconds)
    private static final long MIN_INTERVAL_MS = 2 * 60 * 1000;  // 2 minutes
    private static final long MAX_INTERVAL_MS = 150 * 1000;      // 2.5 minutes
    private static final int JUMP_DURATION_TICKS = 3;            // How long to hold jump (3 ticks = ~150ms)

    public static void enable() {
        enabled = true;
        scheduleNextJump();
    }

    public static void disable() {
        enabled = false;
        jumpTicksRemaining = 0;
        wasScreenOpen = false;
        reopenTime = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.jumpKey.setPressed(false);
        }
    }
    
    private static void scheduleNextJump() {
        long intervalMs = MIN_INTERVAL_MS + random.nextLong(MAX_INTERVAL_MS - MIN_INTERVAL_MS);
        nextJumpTime = System.currentTimeMillis() + intervalMs;
    }

    public static void onTick(MinecraftClient client) {
        if (!enabled || client.player == null) {
            return;
        }

        // Handle delayed reopening
        if (reopenTime > 0) {
            if (System.currentTimeMillis() >= reopenTime) {
                if (client.getNetworkHandler() != null) {
                     client.getNetworkHandler().sendChatCommand("ah");
                }
                reopenTime = 0;
            }
            return; // Wait for reopen before doing anything else
        }
        
        // JUMP SEQUENCE
        if (jumpTicksRemaining > 0) {
            client.options.jumpKey.setPressed(true);
            jumpTicksRemaining--;
            
            // End of jump
            if (jumpTicksRemaining == 0) {
                client.options.jumpKey.setPressed(false);
                
                // If we forced screen close, schedule reopen
                if (wasScreenOpen) {
                    reopenTime = System.currentTimeMillis() + 1500; // 1.5 second delay
                    wasScreenOpen = false;
                }
            }
            return;
        }
        
        // CHECK TIMER
        if (System.currentTimeMillis() >= nextJumpTime) {
            // Check if screen is open (except our chat/menu)
            if (client.currentScreen != null) {
                wasScreenOpen = true;
                client.setScreen(null); // Close screen to allow jump
            } else {
                wasScreenOpen = false;
            }
            
            jumpTicksRemaining = JUMP_DURATION_TICKS;
            scheduleNextJump();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
