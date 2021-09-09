package me.zeddit.graves;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class GraveLogger {

    private final StringBuffer buffer;
    private File dataFile;
    private final ReentrantLock lock = new ReentrantLock();
    private final boolean enabled;
    private static final PlainTextComponentSerializer serializer = PlainTextComponentSerializer.builder().build();

    public GraveLogger(int capacity) {
        dataFile = new File(String.format("%s/logs/", GravesMain.getInstance().getDataFolder()));
        buffer = new StringBuffer(capacity);
        if (!dataFile.exists()) {
            try {
                if (!dataFile.mkdirs()) {
                    throw new IOException("Could not create dirs!");
                }
                dataFile = new File(dataFile, String.format("graves%s.log",dateToday()));
                if (!dataFile.createNewFile()) {
                    throw new IOException("Could not create new graves.log file!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                enabled = false;
                return;
            }
        }
        dataFile = new File(dataFile, String.format("graves%s.log",dateToday()));
        enabled = true;
    }
    private String dateToday() {
        return new SimpleDateFormat("dd-MM-yy").format(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
    }
    public static String playerName(Player player) {
        return serializer.serialize(player.displayName());
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
        builder.append(String.format("Grave Created by creator:%s (creatoruuid:%s) at %s with items %s", playerName(creator), creator.getUniqueId(),formatLocation(location), formatItems(contents)));
        final String log = builder.toString();
        append(log);
    }

    public void logOpen(Player opener, Location location, UUID originalOwner, List<ItemStack> contents) {
        checkEnabled();
        final StringBuilder builder = baseLog();
        builder.append(String.format("Grave owned by originalowneruuid:%s Opened by opener:%s (openeruuid:%s) at %s with items %s", originalOwner, playerName(opener), opener.getUniqueId(), formatLocation(location), formatItems(contents)));
        final String log = builder.toString();
        append(log);
    }

    public void logExpiry(UUID owner) {
        checkEnabled();
        final StringBuilder builder = baseLog();
        append(builder.append(String.format("Grave owned by owneruuid:%s expired.", owner)).toString());
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

    public synchronized void queueFlush() {
        final String toFlush = buffer.toString();
        if (toFlush.equals("")) return;
        buffer.setLength(0);
        GravesMain.getInstance().getService().submit(() -> {
            try(final FileWriter writer = new FileWriter(dataFile, true)) {
                lock.lock();
                writer.append(toFlush).append("\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        });
    }

}
