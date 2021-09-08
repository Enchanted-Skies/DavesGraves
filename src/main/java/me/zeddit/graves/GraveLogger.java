package me.zeddit.graves;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class GraveLogger {

    private final StringBuffer buffer;
    private final File dataFile = new File(GravesMain.getInstance().getDataFolder(), "graves.log");
    private final ReentrantLock lock = new ReentrantLock();
    private final boolean enabled;

    public GraveLogger(int capacity) {
        buffer = new StringBuffer(capacity);
        if (!dataFile.exists()) {
            try {
                if (!dataFile.createNewFile()) {
                    throw new IOException("Could not create new graves.log file!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                enabled = false;
                return;
            }
        }
        enabled = true;
    }

    public void logRaw(String msg) {
        checkEnabled();
        final String log = baseLog().append(msg).toString();
        if (log.length() >= buffer.capacity()) {
            throw new UnsupportedOperationException("Message is too big!");
        }
        append(log);
    }

    public void logCreate(Player creator, Location location, List<ItemStack> contents) {
        checkEnabled();
        final StringBuilder builder = baseLog();
        builder.append(String.format("Grave Created by creator:%s (creatoruuid:%s) at %s with items %s", creator.displayName(), creator.getUniqueId(),formatLocation(location), formatItems(contents)));
        final String log = builder.toString();
    }

    public void logOpen(Player opener, Location location, UUID originalOwner, List<ItemStack> contents) {
        checkEnabled();
        final StringBuilder builder = baseLog();
        builder.append(String.format("Grave owned by originalowneruuid:%s Opened by opener:%s (openeruuid:%s) at %s with items %s", originalOwner, opener.displayName(), opener.getUniqueId(), formatLocation(location), formatItems(contents)));
        final String log = builder.toString();

    }

    private void append(String log) {
        if (log.length() + buffer.length() >= buffer.capacity()) {
            queueFlush();
        }
        buffer.append(log);
    }

    private void checkEnabled(){
        if (!enabled) {
            throw new UnsupportedOperationException("Logging isn't enabled!");
        }
    }

    private StringBuilder baseLog() {
        Instant instant = Instant.now();
        final ZonedDateTime time = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
        return new StringBuilder().append(time).append(" ");
    }

    private String formatItems(List<ItemStack> contents) {
        final StringBuilder builder = new StringBuilder();
        builder.append("contents:");
        contents.forEach(it -> builder.append(it.getAmount()).append("x").append(it.getType()).append(","));
        return builder.substring(0, builder.length() - 1);
    }

    private String formatLocation(Location loc) {
        return String.format("location:%s, %s, %s", Math.round(loc.getX()), Math.round(loc.getY()), Math.round(loc.getZ()));
    }

    private void queueFlush() {
        final String toFlush = buffer.toString();
        buffer.setLength(0);
        GravesMain.getInstance().getService().submit(() -> {
            try(final FileWriter writer = new FileWriter(dataFile)) {
                lock.lock();
                writer.append(toFlush);
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                lock.unlock();
            }
        });
    }

}
