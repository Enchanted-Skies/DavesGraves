package me.zeddit.graves.serialisation;

import org.bukkit.inventory.ItemStack;
    /*
    Serialisation- how it works. At the beginning of the byte array there is a section of bytes that the parser should read and sum
    until a -1 value is reached. The sum should be the start index of the next
     */
import java.util.ArrayList;
import java.util.List;

public class GraveSerialiser {

    private final List<ItemStack> items;

    public GraveSerialiser(List<ItemStack> inv) {
        items = inv;
    }

    public byte[] serialise() {
        final ArrayList<Byte> bytes = new ArrayList<>();
        for
    }
}
