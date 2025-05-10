package eu.koolfreedom.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandParameters
{
	String name();

	String description();

	String usage() default "/<command>";

	String permission() default "";

	String[] aliases() default {};
}
