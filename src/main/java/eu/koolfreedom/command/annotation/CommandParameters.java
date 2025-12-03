package eu.koolfreedom.command.annotation;

import eu.koolfreedom.command.KoolCommand;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <h1>CommandParameters</h1>
 * <p>A series of parameters for {@link KoolCommand} instances to use during the setup process, such as name,
 * description, usage, permission node, and aliases. This is required to be present when initializing commands with the
 * empty constructor, as there is no other way to get that information.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandParameters
{
	/**
	 * The name of the command.
	 * @return	String
	 */
	String name();

	/**
	 * The command's description.
	 * @return	String
	 */
	String description();

	/**
	 * The command's usage. For dynamic usage messages that do not care about the alias one uses, the command name
	 * 	can be set to {@code <command>}.
	 * @return	String
	 */
	String usage() default "/<command>";

	/**
	 * The command's permission node. This is automatically set to {@code kfc.command.<command name>}, but can be set to
	 * 	something custom if you like.
	 * @return	String
	 */
	String permission() default "";

	/**
	 * The command's aliases, if any. This does not need to be set as it defaults to an empty array.
	 * @return	String[]
	 */
	String[] aliases() default {};
}
