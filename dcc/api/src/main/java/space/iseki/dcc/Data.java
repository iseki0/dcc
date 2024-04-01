package space.iseki.dcc;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class Data {
    @SuppressWarnings("Contract")
    @Contract("!null -> !null; null -> fail") // override the default analyze
    public static <T> @NotNull Codec<T> getCodec(@NotNull Class<T> type) {
        throw new AssertionError("intrinsic");
    }
}
