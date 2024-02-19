package space.iseki.dcc;


/**
 * <b>This is an internal class, never use it directly.</b>
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public record FieldMirrorRecord(String name, Class<?> type, boolean optional) implements FieldMirror {
}
