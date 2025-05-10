package eu.koolfreedom.log;

import eu.koolfreedom.KoolSMPCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.slf4j.Logger;

public class FLog
{
    private static final Logger logger = KoolSMPCore.getInstance().getSLF4JLogger();
    private static final ComponentLogger componentLogger = KoolSMPCore.getInstance().getComponentLogger();

    public static void info(String message)
    {
        logger.info(message);
    }

    public static void info(String message, Object... args)
    {
        logger.info(message, args);
    }

    public static void info(Component message)
    {
        componentLogger.info(message);
    }

    public static void warning(String message)
    {
        logger.warn(message);
    }

    public static void warning(String message, Object... args)
    {
        logger.warn(message, args);
    }

    public static void warning(Component message)
    {
        componentLogger.warn(message);
    }

    public static void warning(String message, Throwable ex)
    {
        logger.warn(message, ex);
    }

    public static void warning(Component message, Throwable ex)
    {
        componentLogger.warn(message, ex);
    }

    public static void error(String message)
    {
        logger.error(message);
    }

    public static void error(String message, Object... args)
    {
        logger.error(message, args);
    }

    public static void error(Component message)
    {
        componentLogger.error(message);
    }

    public static void error(String message, Throwable ex)
    {
        logger.error(message, ex);
    }

    public static void error(Component message, Throwable ex)
    {
        componentLogger.error(message, ex);
    }
}