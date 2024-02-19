package space.iseki.dcc;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class Data {
    public static <T> @NotNull Codec<T> getCodec(@NotNull Class<T> type) {
        throw new AssertionError("intrinsic");
    }
}
