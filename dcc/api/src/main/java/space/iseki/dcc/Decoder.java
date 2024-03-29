package space.iseki.dcc;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface Decoder {
    default Object getObject(@NotNull Codec<?> codec, int index,@NotNull  Class<?> type) {
        return getObject(codec, codec.getNameByIndex(index), type);
    }

    Object getObject(@NotNull Codec<?> codec, @NotNull String name, @NotNull Class<?> type);


    default byte getByte(@NotNull Codec<?> codec, @NotNull String name) {
        return (Byte) getObject(codec, name, Byte.class);
    }

    default char getChar(@NotNull Codec<?> codec, @NotNull String name) {
        return (Character) getObject(codec, name, Character.class);
    }

    default short getShort(@NotNull Codec<?> codec, @NotNull String name) {
        return (Short) getObject(codec, name, Short.class);
    }

    default int getInt(@NotNull Codec<?> codec, @NotNull String name) {
        return (Integer) getObject(codec, name, Integer.class);
    }

    default long getLong(@NotNull Codec<?> codec, @NotNull String name) {
        return (Long) getObject(codec, name, Long.class);
    }

    default float getFloat(@NotNull Codec<?> codec, @NotNull String name) {
        return (Float) getObject(codec, name, Float.class);
    }

    default double getDouble(@NotNull Codec<?> codec, @NotNull String name) {
        return (Double) getObject(codec, name, Double.class);
    }

    default boolean getBoolean(@NotNull Codec<?> codec, @NotNull String name) {
        return (Boolean) getObject(codec, name, Boolean.class);
    }

    default byte getByte(@NotNull Codec<?> codec, int index) {
        return getByte(codec, codec.getNameByIndex(index));
    }

    default char getChar(@NotNull Codec<?> codec, int index) {
        return getChar(codec, codec.getNameByIndex(index));
    }

    default short getShort(@NotNull Codec<?> codec, int index) {
        return getShort(codec, codec.getNameByIndex(index));
    }

    default int getInt(@NotNull Codec<?> codec, int index) {
        return getInt(codec, codec.getNameByIndex(index));
    }

    default long getLong(@NotNull Codec<?> codec, int index) {
        return getLong(codec, codec.getNameByIndex(index));
    }

    default float getFloat(@NotNull Codec<?> codec, int index) {
        return getFloat(codec, codec.getNameByIndex(index));
    }

    default double getDouble(@NotNull Codec<?> codec, int index) {
        return getDouble(codec, codec.getNameByIndex(index));
    }

    default boolean getBoolean(@NotNull Codec<?> codec, int index) {
        return getBoolean(codec, codec.getNameByIndex(index));
    }

    boolean isDefault(@NotNull Codec<?> codec, @NotNull String name);

    default boolean isDefault(@NotNull Codec<?> codec, int index) {
        return isDefault(codec, codec.getNameByIndex(index));
    }
}
