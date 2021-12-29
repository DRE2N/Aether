package de.erethon.aether.tools;

import de.erethon.commons.chat.FatLetter;
import de.erethon.commons.javaplugin.DREPlugin;
import de.erethon.commons.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.16+ Version of {@link de.erethon.commons.chat.MessageUtil}
 *
 * @author Sataniel, Fyreum
 */
public class UpdatedMessageUtil {

    private static final MiniMessage mm = MiniMessage.get();
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FK-ORX]");

    /**
     * Logs a message to the console. Supports color codes.
     *
     * @param message the message String
     */
    public static void log(String message) {
        log(DREPlugin.getInstance(), message);
    }

    /**
     * Logs a message to the console. Supports color codes.
     *
     * @param plugin  the logging plugin
     * @param message the message String
     */
    public static void log(Plugin plugin, String message) {
        Bukkit.getConsoleSender().sendMessage("[" + plugin.getName() + "] " + color(message));
    }

    /**
     * Logs a message to the console.
     *
     * @param message the message Component
     */
    public static void log(Component message) {
        log(DREPlugin.getInstance(), message);
    }

    /**
     * Logs a message to the console.
     *
     * @param plugin  the logging plugin
     * @param message the message Component
     */
    public static void log(Plugin plugin, Component message) {
        Bukkit.getConsoleSender().sendMessage(Component.text("[" + plugin.getName() + "] ").append(message));
    }

    /**
     * Broadcasts a message to all players. Supports color codes.
     *
     * @param message the message String
     */
    public static void broadcastMessage(String message) {
        broadcastMessage(parse(message));
    }

    /**
     * Broadcasts the Component to all players.
     *
     * @param message the message component
     */
    public static void broadcastMessage(Component message) {
        Bukkit.getOnlinePlayers().forEach((p) -> sendMessage(p, message));
    }

    /**
     * Broadcasts a perfectly centered message to all players. Supports color codes.
     * (Component formatting like <COLOR> or <rainbow> can cause to issues)
     *
     * @param message the message String
     */
    public static void broadcastCenteredMessage(String message) {
        broadcastMessage(UpdatedDefaultFontInfo.center(message));
    }

    /**
     * Broadcasts a perfectly centered Component message to all players.
     * (Component formatting like <COLOR> or <rainbow> can cause to issues)
     *
     * @param message the message String
     */
    public static void broadcastCenteredMessage(Component message) {
        broadcastCenteredMessage(serialize(message));
    }

    /**
     * Broadcasts the plugin name formatted to a player (or another sender), for example as a headline.
     *
     * @param plugin the plugin
     */
    public static void broadcastPluginTag(Plugin plugin) {
        broadcastCenteredMessage(getPluginTag(plugin));
    }

    /**
     * Broadcasts a title message. Supports color codes.
     *
     * @param title    the message of the first, big line
     * @param subtitle the message of the second, small line
     * @param fadeIn   the time in ticks it takes for the message to appear
     * @param show     the time in ticks how long the message will be visible
     * @param fadeOut  the time in ticks it takes for the message to disappear
     */
    public static void broadcastTitleMessage(String title, String subtitle, int fadeIn, int show, int fadeOut) {
        Bukkit.getOnlinePlayers().forEach((p) -> sendTitleMessage(p, title, subtitle, fadeIn, show, fadeOut));
    }

    /**
     * Broadcasts a title message. Supports color codes.
     *
     * @param title    the message of the first, big line
     * @param subtitle the message of the second, small line
     */
    public static void broadcastTitleMessage(String title, String subtitle) {
        broadcastTitleMessage(title, subtitle, 20, 60, 20);
    }

    /**
     * Broadcasts a title message. Supports color codes.
     *
     * @param title the message of the first, big line
     */
    public static void broadcastTitleMessage(String title) {
        broadcastTitleMessage(title, "", 20, 60, 20);
    }

    /**
     * Broadcasts an action bar message. Supports color codes.
     *
     * @param message the message String
     */
    public static void broadcastActionBarMessage(String message) {
        broadcastActionBarMessage(parse(message));
    }

    /**
     * Broadcasts an action bar message.
     *
     * @param message the message components
     */
    public static void broadcastActionBarMessage(Component message) {
        Bukkit.getOnlinePlayers().forEach((p) -> sendActionBarMessage(p, message));
    }

    /**
     * Broadcasts a fat message Does not support color codes.
     *
     * @param color the color of the message
     * @param word  the word to send
     */
    public static void broadcastFatMessage(ChatColor color, String word) {
        word = ChatColor.translateAlternateColorCodes('&', word);
        word = ChatColor.stripColor(word);
        String[] fat = FatLetter.fromString(word);

        for (Player player : Bukkit.getOnlinePlayers()) {
            sendCenteredMessage(player, color + fat[0]);
            sendCenteredMessage(player, (color + fat[1]));
            sendCenteredMessage(player, (color + fat[2]));
            sendCenteredMessage(player, (color + fat[3]));
            sendCenteredMessage(player, (color + fat[4]));
        }
    }

    /**
     * Sends a message to a specific player (or another CommandSender). Supports color codes.
     *
     * @param sender  the sender
     * @param message the message String
     */
    public static void sendMessage(CommandSender sender, String message) {
        sendMessage(sender, parse(message));
    }

    /**
     * Sends a Component message to a specific player (or another CommandSender).
     *
     * @param sender  the sender
     * @param message the message component
     */
    public static void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }

    /**
     * Sends a perfectly centered message to a specific player (or another CommandSender). Supports color codes.
     *
     * @param sender  the sender
     * @param message the message String
     */
    public static void sendCenteredMessage(CommandSender sender, String message) {
        sendMessage(sender, UpdatedDefaultFontInfo.center(message));
    }

    /**
     * Sends a perfectly centered Component message to a specific player (or another CommandSender).
     *
     * @param sender  the sender
     * @param message the message component
     */
    public static void sendCenteredMessage(CommandSender sender, Component message) {
        sendCenteredMessage(sender, serialize(message));
    }

    /**
     * Sends the plugin name formatted to a player (or another sender), for example as a headline.
     *
     * @param sender the sender
     * @param plugin the plugin
     */
    public static void sendPluginTag(CommandSender sender, Plugin plugin) {
        sendCenteredMessage(sender, getPluginTag(plugin));
    }

    /**
     * Returns the formatted plugin name.
     *
     * @param plugin the plugin
     * @return the formatted plugin name
     */
    public static String getPluginTag(Plugin plugin) {
        return "&4&l[ &6" + plugin.getDescription().getName() + " &4&l]";
    }

    /**
     * Sends a title message. Supports color codes.
     *
     * @param player   the player who will receive the message
     * @param title    the message of the first, big line
     * @param subtitle the message of the second, small line
     * @param fadeIn   the time in ticks it takes for the message to appear
     * @param show     the time in ticks how long the message will be visible
     * @param fadeOut  the time in ticks it takes for the message to disappear
     */
    public static void sendTitleMessage(Player player, String title, String subtitle, int fadeIn, int show, int fadeOut) {
        title = serialize(parse(title));
        subtitle = serialize(parse(subtitle));
        player.sendTitle(title, subtitle, fadeIn, show, fadeOut);
    }

    /**
     * Sends a title message. Supports color codes.
     *
     * @param player   the player who will receive the message
     * @param title    the message of the first, big line
     * @param subtitle the message of the second, small line
     */
    public static void sendTitleMessage(Player player, String title, String subtitle) {
        sendTitleMessage(player, title, subtitle, 20, 60, 20);
    }

    /**
     * Sends a title message. Supports color codes.
     *
     * @param player the player who will receive the message
     * @param title  the message of the first, big line
     */
    public static void sendTitleMessage(Player player, String title) {
        sendTitleMessage(player, title, "", 20, 60, 20);
    }

    /**
     * Sends an action bar message. Supports color codes.
     *
     * @param player  the player who will receive the message
     * @param message the message String
     */
    public static void sendActionBarMessage(Player player, String message) {
        sendActionBarMessage(player, parse(message));
    }

    /**
     * Sends an action bar message.
     *
     * @param player  the player who will receive the message
     * @param message the message components
     */
    public static void sendActionBarMessage(Player player, Component message) {
        player.sendActionBar(message);
    }

    /**
     * Sends a fat message. Does not support color codes.
     *
     * @param player the player who will receive the message
     * @param color  the color of the message
     * @param word   the word to send
     */
    public static void sendFatMessage(Player player, ChatColor color, String word) {
        word = ChatColor.translateAlternateColorCodes('&', word);
        word = ChatColor.stripColor(word);
        String[] fat = FatLetter.fromString(word);
        sendCenteredMessage(player, color + fat[0]);
        sendCenteredMessage(player, color + fat[1]);
        sendCenteredMessage(player, color + fat[2]);
        sendCenteredMessage(player, color + fat[3]);
        sendCenteredMessage(player, color + fat[4]);
    }

    /**
     * Parses the string.
     * <p>
     * Translates color codes and MiniMessage tags
     *
     * @param msg the message to parse
     * @return the parsed Component
     */
    public static Component parse(String msg) {
        return parse(mm, msg);
    }

    /**
     * Parses the string.
     * <p>
     * Translates color codes and MiniMessage tags
     *
     * @param mm the MiniMessage instance to use
     * @param msg the message to parse
     * @return the parsed Component
     */
    public static Component parse(MiniMessage mm, String msg) {
        String fixed = fixColor(msg);
        String translated = ChatColor.translateAlternateColorCodes('&', fixed);
        return (Component) mm.parse(translated);
    }

    /**
     * Serializes the msg component.
     *
     * @param msg the message to serialize
     * @return the serialized string
     */
    public static String serialize(Component msg) {
        return mm.serialize((de.erethon.commons.kyori.adventure.text.Component) msg);
    }

    /**
     * Calls the {@link MiniMessage#stripTokens(String input)} method.
     *
     * @param input the string to strip
     * @return the striped input
     */
    public static String stripTokens(String input) {
        return mm.stripTokens(input);
    }

    /**
     * Strips all translated and untranslated color codes.
     *
     * @param input the string to strip
     * @return the striped input
     */
    public static String stripColor(String input) {
        return ChatColor.stripColor(STRIP_COLOR_PATTERN.matcher(input).replaceAll(""));
    }

    /**
     * Fixes missing color codes due to MiniMessage formatting with '<' and '>'.
     * <p>
     * MiniMessage has erroneous formatting which causes to missing color codes in strings.
     * So unused formatting chars like '<' and '>' would be white, no matter of the correct color given.
     *
     * @param msg the message to fix
     * @return the fixes string
     */
    public static String fixColor(String msg) {
        char[] chars = msg.toCharArray();
        ChatColor color = null;

        for (int i = 0; i < chars.length -2; i++) {
            char current = chars[i];
            int nextIndex = i + 1;
            if (current == '&' | current == ChatColor.COLOR_CHAR) {
                char next = chars[nextIndex];
                ChatColor newColor = ChatColor.getByChar(next);
                if (newColor != null) {
                    color = newColor;
                }
            }
            if (color == null) {
                continue;
            }
            if (chars[nextIndex + 1] != '<') {
                continue;
            }
            String start = msg.substring(0, nextIndex);
            String subString = msg.substring(nextIndex).replaceFirst(">", ">&" + color.getChar());

            msg = start + subString;
            chars = msg.toCharArray();
            i = nextIndex + 1;
        }
        return msg;
    }

    /**
     * Colors the string.
     * <p>
     * Translates color codes and in 1.16+ hex color as well.
     *
     * @param string the String to parse
     * @return the colored string
     */
    public static String color(String string) {
        Matcher match = HEX_COLOR_PATTERN.matcher(string);
        while (match.find()) {
            String color = string.substring(match.start(), match.end());
            string = string.replace(color, net.md_5.bungee.api.ChatColor.of(color) + "");
            match = HEX_COLOR_PATTERN.matcher(string);
        }
        return ChatColor.translateAlternateColorCodes('&', string);
    }

}