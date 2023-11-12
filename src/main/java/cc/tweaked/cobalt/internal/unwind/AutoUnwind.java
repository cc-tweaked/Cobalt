package cc.tweaked.cobalt.internal.unwind;

import org.squiddev.cobalt.unwind.SuspendedTask;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be applied to a method to automatically convert it into a suspendable function, i.e. one which
 * can yield and be resumed.
 * <p>
 * If this method is annotated, it should <em>NOT</em> be called directly, but instead via one of the helper methods
 * such as {@link SuspendedTask}
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AutoUnwind {
}
