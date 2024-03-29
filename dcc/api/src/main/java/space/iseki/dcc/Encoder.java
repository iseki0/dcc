package space.iseki.dcc;

import org.jetbrains.annotations.NotNull;


@SuppressWarnings("unused")
public interface Encoder {
    /*
    byte
    char
    short
    int
    long
    float
    double
    boolean

    Object getObject(@NotNull Codec<?> codec, int index);
    Object getObject(@NotNull Codec<?> codec, @NotNull String name);

    default byte getByte(@NotNull Codec<?> codec, @NotNull String name){getByte(codec, name);};
    default char getChar(@NotNull Codec<?> codec, @NotNull String name){getChar(codec, name);};
    default short getShort(@NotNull Codec<?> codec, @NotNull String name){getShort(codec, name);};
    default int getInt(@NotNull Codec<?> codec, @NotNull String name){getInt(codec, name);};
    default long getLong(@NotNull Codec<?> codec, @NotNull String name){getLong(codec, name);};
    default float getFloat(@NotNull Codec<?> codec, @NotNull String name){getFloat(codec, name);};
    default double getDouble(@NotNull Codec<?> codec, @NotNull String name){getDouble(codec, name);};
    default boolean getBoolean(@NotNull Codec<?> codec, @NotNull String name){getBoolean(codec, name);};

    default byte getByte(@NotNull Codec<?> codec, int index){getByte(codec, codec.getNameByIndex(index));};
    default char getChar(@NotNull Codec<?> codec, int index){getChar(codec, codec.getNameByIndex(index));};
    default short getShort(@NotNull Codec<?> codec, int index){getShort(codec, codec.getNameByIndex(index));};
    default int getInt(@NotNull Codec<?> codec, int index){getInt(codec, codec.getNameByIndex(index));};
    default long getLong(@NotNull Codec<?> codec, int index){getLong(codec, codec.getNameByIndex(index));};
    default float getFloat(@NotNull Codec<?> codec, int index){getFloat(codec, codec.getNameByIndex(index));};
    default double getDouble(@NotNull Codec<?> codec, int index){getDouble(codec, codec.getNameByIndex(index));};
    default boolean getBoolean(@NotNull Codec<?> codec, int index){getBoolean(codec, codec.getNameByIndex(index));};
     */

    ///////////////////////////////////////////////////////////////////////////
    /////// methods
    ///////////////////////////////////////////////////////////////////////////

    void setObject(@NotNull Codec<?> codec, @NotNull String name, Object value);

    default void setObject(@NotNull Codec<?> codec, int index, Object value) {
        setObject(codec, codec.getNameByIndex(index), value);
    }

    default void setByte(@NotNull Codec<?> codec, @NotNull String name, byte value) {
        setObject(codec, name, value);
    }

    default void setChar(@NotNull Codec<?> codec, @NotNull String name, char value) {
        setObject(codec, name, value);
    }

    default void setShort(@NotNull Codec<?> codec, @NotNull String name, short value) {
        setObject(codec, name, value);
    }

    default void setInt(@NotNull Codec<?> codec, @NotNull String name, int value) {
        setObject(codec, name, value);
    }

    default void setLong(@NotNull Codec<?> codec, @NotNull String name, long value) {
        setObject(codec, name, value);
    }

    default void setFloat(@NotNull Codec<?> codec, @NotNull String name, float value) {
        setObject(codec, name, value);
    }

    default void setDouble(@NotNull Codec<?> codec, @NotNull String name, double value) {
        setObject(codec, name, value);
    }

    default void setBoolean(@NotNull Codec<?> codec, @NotNull String name, boolean value) {
        setObject(codec, name, value);
    }

    default void setByte(@NotNull Codec<?> codec, int index, byte value) {
        setByte(codec, codec.getNameByIndex(index), value);
    }

    default void setChar(@NotNull Codec<?> codec, int index, char value) {
        setChar(codec, codec.getNameByIndex(index), value);
    }

    default void setShort(@NotNull Codec<?> codec, int index, short value) {
        setShort(codec, codec.getNameByIndex(index), value);
    }

    default void setInt(@NotNull Codec<?> codec, int index, int value) {
        setInt(codec, codec.getNameByIndex(index), value);
    }

    default void setLong(@NotNull Codec<?> codec, int index, long value) {
        setLong(codec, codec.getNameByIndex(index), value);
    }

    default void setFloat(@NotNull Codec<?> codec, int index, float value) {
        setFloat(codec, codec.getNameByIndex(index), value);
    }

    default void setDouble(@NotNull Codec<?> codec, int index, double value) {
        setDouble(codec, codec.getNameByIndex(index), value);
    }

    default void setBoolean(@NotNull Codec<?> codec, int index, boolean value) {
        setBoolean(codec, codec.getNameByIndex(index), value);
    }

}
