package me.zeddit.graves.serialisation;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/*
Serialisation- how it works. At the beginning of the byte array there is a section of bytes that the parser should read and sum
until a -1 value is reached. The sum should be the start index of the next 'index of bytes' section. All bytes up until that section are the serialised itemstack.
When the index of bytes section is just a -1, this means there is nothing else to read.
*/

public class GraveSerialiser {

    private final List<byte[]> items;

    public GraveSerialiser(@NotNull List<ItemStack> inv) {
        items = inv.stream().map(ItemStack::serializeAsBytes).collect(Collectors.toList());
    }

    public byte[] serialise() {
        final ArrayList<Byte> bytes = new ArrayList<>();
        for (byte[] item : items) {
            addIndexSection(item.length, bytes);
            for (byte b: item) {
                bytes.add(b);
            }
        }
        bytes.add((byte)-1);
        return byteListToArr(bytes);
    }

    //mutates list
    private void addIndexSection(int i, List<Byte> bytes) {
        final byte[] indexBytes = intToBytes(i);
        for (byte b: indexBytes) {
            bytes.add(b);
        }
        bytes.add((byte)-1);
    }

    public static List<ItemStack> deserialise(byte[] bytes) { // a do while loop might be more elegant here but im rly tired
        final List<ItemStack> inv = new ArrayList<>();
        int i = 0;
        int sum = 0;
        int top = 0;
        List<Byte> currentStack = new ArrayList<>();
        while (i < bytes.length) {
            if (bytes[i] == -1 && sum != -1) {
                if (i == bytes.length -1) {
                    break;
                }
                top = sum+1;
                sum = -1;
            }
            if (sum == -1) {
                if (i == top) {
                    sum = 0;
                    inv.add(ItemStack.deserializeBytes(byteListToArr(currentStack)));
                    continue;
                }
                currentStack.add(bytes[i]);
            } else {
                sum += bytes[i];
            }
            i++;
        }
        return inv;
    }

    private static byte[] intToBytes(int i) {
        final ArrayList<Byte> bytes = new ArrayList<>();
        for (int j = 0; j < i / 127; j++) {
            bytes.add(Byte.MAX_VALUE);
        }
        bytes.add((byte) (i % 127));
        final byte[] finalBytes = new byte[bytes.size()];
        for (int j = 0; j < finalBytes.length; j++) {
            finalBytes[j] = bytes.get(j);
        }
        return finalBytes;
    }
    //DO NOT include the last -1
    private static int bytesToInt(byte[] bytes) {
        int sum = 0;
        for (byte b: bytes) {
            sum += b;
        }
        return sum;
    }

    private static List<Byte> byteArrToList(byte[] bytes) {
        final List<Byte> list = new ArrayList<>();
        for (byte b: bytes) {
            list.add(b);
        }
        return list;
    }

    private static byte[] byteListToArr(List<Byte> list) {
        final byte[] bytes = new byte[list.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = list.get(i);
        }
        return bytes;
    }
}
