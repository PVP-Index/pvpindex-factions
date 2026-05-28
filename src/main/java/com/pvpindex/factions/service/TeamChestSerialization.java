package com.pvpindex.factions.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

/**
 * Utility methods for serializing team chest contents.
 */
final class TeamChestSerialization {

    private TeamChestSerialization() {
    }

    static String encode(final List<ItemStack> items) throws IOException {
        final List<ItemStack> safeItems = items == null ? List.of() : items;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bos)) {
            out.writeInt(safeItems.size());
            for (final ItemStack item : safeItems) {
                out.writeObject(item);
            }
            out.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        }
    }

    @SuppressWarnings("unchecked")
    static List<ItemStack> decode(final String encoded) throws IOException, ClassNotFoundException {
        if (encoded == null || encoded.isBlank()) {
            return new ArrayList<>();
        }
        final byte[] raw = Base64.getDecoder().decode(encoded);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(raw);
             BukkitObjectInputStream in = new BukkitObjectInputStream(bis)) {
            final int size = in.readInt();
            final List<ItemStack> out = new ArrayList<>(Math.max(size, 0));
            for (int i = 0; i < size; i++) {
                out.add((ItemStack) in.readObject());
            }
            return out;
        }
    }
}
