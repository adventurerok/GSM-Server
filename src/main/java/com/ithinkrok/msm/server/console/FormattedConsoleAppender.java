package com.ithinkrok.msm.server.console;

import com.google.common.base.Charsets;
import com.ithinkrok.util.config.Config;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

/**
 * Created by paul on 21/02/16.
 */
@Plugin(name = "FormattedConsole", category = "core", elementType = "appender", printObject = true)
public class FormattedConsoleAppender extends AbstractAppender {


    private final Object lock = new Object();

    protected FormattedConsoleAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                                       boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @PluginFactory
    public static FormattedConsoleAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Layout")
    Layout<? extends Serializable> layout, @PluginElement("Filter") final Filter filter) {
        if (name == null) {
            LOGGER.error("No name provided for MyCustomAppenderImpl");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new FormattedConsoleAppender(name, filter, layout, true);
    }

    @Override
    public void append(LogEvent event) {
        synchronized (lock) {
            try {
                final byte[] bytes = getLayout().toByteArray(event);

                if(event.getLevel() == Level.WARN) {
                    ConsoleHandler.out.write("§c".getBytes(Charsets.UTF_8));
                } else if(event.getLevel() == Level.ERROR || event.getLevel() == Level.FATAL) {
                    ConsoleHandler.out.write("§4".getBytes(Charsets.UTF_8));
                } else if(event.getLevel() == Level.DEBUG) {
                    ConsoleHandler.out.write("§3".getBytes(Charsets.UTF_8));
                }


                ConsoleHandler.out.write(bytes);
                ConsoleHandler.out.flush();
            } catch (Exception e) {
                if (!ignoreExceptions()) {
                    throw new AppenderLoggingException(e);
                }
            }
        }
    }
}
