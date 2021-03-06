
package chatty;

import chatty.util.DateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Twitch Chat commands. All the Twitch specific commands like /mod, /timeout..
 * 
 * @author tduva
 */
public class TwitchCommands {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchCommands.class.getName());
    
    /**
     * The delay between /mods requests. This is the delay in between each
     * request, not how often it is requested for one channel (it is currently
     * only requested once for each channel).
     */
    private static final int REQUEST_MODS_DELAY = 30*1000;
    
    /**
     * Channels which currently wait for a /mods response that should be silent
     * (no message output).
     */
    private final Set<String> silentModsRequestChannel
            = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Channels for which the /mods list has already been requested.
     */
    private final Set<String> modsAlreadyRequested
            = Collections.synchronizedSet(new HashSet<String>());
    
    private TwitchConnection c;
    
    public TwitchCommands(TwitchConnection c) {
        this.c = c;
    }
    
    public boolean command(String channel, String command, String parameter) {
       if (command.equals("to") || command.equals("timeout")) {
            commandTimeout(channel,parameter);
        }
        else if (command.equals("unban")) {
            commandUnban(channel, parameter);
        }
        else if (command.equals("ban")) {
            commandBan(channel, parameter);
        }
        else if (command.equals("slow")) {
            commandSlowmodeOn(channel, parameter);
        }
        else if (command.equals("slowoff")) {
            slowmodeOff(channel);
        }
        else if (command.equals("subscribers")) {
            subscribersOn(channel);
        }
        else if (command.equals("subscribersoff")) {
            subscribersOff(channel);
        }
        else if (command.equals("r9k")) {
            r9kOn(channel);
        }
        else if (command.equals("r9koff")) {
            r9kOff(channel);
        }
        else if (command.equals("mod")) {
            commandMod(channel, parameter);
        }
        else if (command.equals("unmod")) {
            commandUnmod(channel, parameter);
        }
        else if (command.equals("clear")) {
            clearChannel(channel);
        }
        else if (command.equals("mods")) {
            mods(channel);
        }
        else if (command.equals("fixmods")) {
            modsSilent(channel);
        }
        else if (command.equals("host")) {
            commandHostmode(channel, parameter);
        }
        else if (command.equals("unhost")) {
            hostmodeOff(channel);
        }
        else if (command.equals("color")) {
            commandColor(channel, parameter);
        }
        else {
            return false;
        }
       return true;
    }
    
    private boolean onChannel(String channel, boolean message) {
        return c.onChannel(channel, message);
    }
    
    private void sendMessage(String channel, String message, String echo) {
        c.sendCommandMessage(channel, message, echo);
    }
    
    private void printLine(String channel, String message) {
        c.info(channel, message);
    }
    
    private void printLine(String message) {
        c.info(message);
    }
    
    protected void commandTimeout(String channel, String parameter) {
        parameter = prepareAndCheckParameters(Helper.USERNAME_REGEX+"( [0-9]+)?", parameter);
        if (parameter == null) {
            printLine("Usage: /to <nick> [time]");
            return;
        }
        String[] parts = parameter.split(" ");
        if (parts.length < 2) {
            timeout(channel, parts[0], 0);
        } else {
            try {
                long time = Long.parseLong(parts[1]);
                timeout(channel, parts[0], time);
            } catch (NumberFormatException ex) {
                // If the regex is correct, this may never happen
                printLine("Usage: /to <nick> [time] (no valid time specified)");
            }
        }
    }
    
    protected void commandSlowmodeOn(String channel, String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            slowmodeOn(channel, 0);
        }
        else {
            try {
                int time = Integer.parseInt(parameter);
                slowmodeOn(channel, time);
            } catch (NumberFormatException ex) {
                printLine("Usage: /slow [time] (invalid time specified)");
            }
        }
    }
    
    protected void commandUnban(String channel, String parameter) {
        if (prepareAndCheckParameters(Helper.USERNAME_REGEX, parameter) == null) {
            printLine("Usage: /unban <nick>");
            return;
        }
        unban(channel, parameter);
    }
    
    protected void commandBan(String channel, String parameter) {
        if (prepareAndCheckParameters(Helper.USERNAME_REGEX, parameter) == null) {
            printLine("Usage: /ban <nick>");
        } else {
            ban(channel, parameter);
        }
    }
    
    protected void commandMod(String channel, String parameter) {
        if (prepareAndCheckParameters(Helper.USERNAME_REGEX, parameter) == null) {
            printLine("Usage: /mod <nick>");
        } else {
            mod(channel, parameter);
        }
    }
    
    protected void commandUnmod(String channel, String parameter) {
        if (prepareAndCheckParameters(Helper.USERNAME_REGEX, parameter) == null) {
            printLine("Usage: /unmod <nick>");
        }
        else {
            unmod(channel, parameter);
        }
    }
    
    protected void commandColor(String channel, String parameter) {
        if (parameter == null) {
            printLine("Usage: /color <newcolor>");
        } else {
            color(channel, parameter);
        }
    }
    
    protected void commandHostmode(String channel, String parameter) {
        if (parameter == null) {
            printLine("Usage: /host <stream>");
        } else {
            hostmode(channel, parameter);
        }
    }
    
    public void hostmode(String channel, String target) {
        if (onChannel(channel, true)) {
            sendMessage(channel, ".host "+target, "Trying to host "+target+"..");
        }
    }
    
    public void hostmodeOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel, ".unhost", "Trying to turn off host mode..");
        }
    }
    
    public void color(String channel, String color) {
        if (onChannel(channel, true)) {
            sendMessage(channel, ".color "+color, "Trying to change color to "+color);
        }
    }
    
    /**
     * Turn on slowmode with the given amount of seconds or the default time
     * (without specifying a time).
     * 
     * @param channel The name of the channel
     * @param time The time in seconds, 0 or negative numbers will make it give
     *  not time at all
     */
    public void slowmodeOn(String channel, int time) {
        if (onChannel(channel, true)) {
            if (time <= 0) {
                sendMessage(channel,".slow", "Trying to turn on slowmode..");
            }
            else {
                sendMessage(channel,".slow "+time, "Trying to turn on slowmode ("+time+"s)");
            }
        }
    }
    
    /**
     * Turns off slowmode in the given channel.
     * 
     * @param channel The name of the channel.
     */
    public void slowmodeOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".slowoff", "Trying to turn off slowmode..");
        }
    }
    
    /**
     * Turns on subscriber only mode in the given channel.
     * 
     * @param channel The name of the channel.
     */
    public void subscribersOn(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".subscribers", "Trying to turn on subscribers mode..");
        }
    }
    
    public void subscribersOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".subscribersoff", "Trying to turn off subscribers mode..");
        }
    }
    
    public void r9kOn(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".r9kbeta", "Trying to turn on r9k mode..");
        }
    }
    
    public void r9kOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".r9kbetaoff", "Trying to turn r9k mode off..");
        }
    }
    
    public void clearChannel(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".clear", "Trying to clear channel..");
        }
    }

    public void ban(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".ban "+name, "Trying to ban "+name+"..");
        }
    }
    
    public void mod(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".mod "+name, "Trying to mod "+name+"..");
        }
    }
    
    public void unmod(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".unmod "+name, "Trying to unmod "+name+"..");
        }
    }
    
    /**
     * Sends a timeout command to the server.
     * 
     * @param channel
     * @param name
     * @param time 
     */
    public void timeout(String channel, String name, long time) {
        if (onChannel(channel, true)) {
            if (time <= 0) {
                sendMessage(channel,".timeout "+name, "Trying to timeout "+name+"..");
            }
            else {
                String formatted = DateTime.duration(time*1000, 0, 2, 0);
                String onlySeconds = time+"s";
                String timeString = formatted.equals(onlySeconds)
                        ? onlySeconds : onlySeconds+"/"+formatted;
                sendMessage(channel,".timeout "+name+" "+time,
                        "Trying to timeout "+name+" ("+timeString+")");
            }
            
        }
    }
    
    public void unban(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".unban "+name, "Trying to unban "+name+"..");
        }
    }
    
    public void mods(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".mods", "Requesting moderator list..");
        }
    }
    
    public void modsSilent(String channel) {
        if (onChannel(channel, true)) {
            printLine(channel, "Trying to fix moderators..");
            requestModsSilent(channel);
        }
    }
    
    public void requestModsSilent(String channel) {
        if (onChannel(channel, false)) {
            silentModsRequestChannel.add(channel);
            c.sendSpamProtectedMessage(channel, ".mods", false);
        }
    }
    
    public boolean removeModsSilent(String channel) {
        return silentModsRequestChannel.remove(channel);
    }
    
    public boolean waitingForModsSilent() {
        return !silentModsRequestChannel.isEmpty();
    }
    
    /**
     * Prase the list of mods as returned from the Twitch Chat. The
     * comma-seperated list should start after the first colon ("The moderators
     * of this room are: ..").
     *
     * @param text The text as received from the Twitch Chat
     * @return A List of moderator names
     */
    public static List<String> parseModsList(String text) {
        int start = text.indexOf(":") + 1;
        List<String> modsList = new ArrayList<>();
        if (start > 1 && text.length() > start) {
            String mods = text.substring(start);
            if (!mods.trim().isEmpty()) {
                String[] modsArray = mods.split(",");
                for (String mod : modsArray) {
                    modsList.add(mod.trim());
                }
            }
        }
        return modsList;
    }
    
    /**
     * Starts the timer which requests the /mods list for joined channels.
     */
    public void startAutoRequestMods() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                autoRequestMods();
            }
        }, 1000, REQUEST_MODS_DELAY);
    }
    
    /**
     * If enabled in the settings, requests /mods for one currently joined
     * channel (and only one), ignoring the ones it was already requested for.
     */
    private void autoRequestMods() {
        if (!c.autoRequestModsEnabled()) {
            return;
        }
        Set<String> joinedChannels = c.getJoinedChannels();
        for (String channel : joinedChannels) {
            if (!modsAlreadyRequested.contains(channel)) {
                LOGGER.info("Auto-requesting mods for "+channel);
                modsAlreadyRequested.add(channel);
                requestModsSilent(channel);
                return;
            }
        }
    }
    
    /**
     * Removes one or all entries from the list of channels the /mods list was
     * already requested for. This can be used on part/disconnect, since users
     * are removed then.
     * 
     * @param channel The name of the channel to remove, or null to remove all
     * entries
     */
    public void clearModsAlreadyRequested(String channel) {
        if (channel == null) {
            modsAlreadyRequested.clear();
        } else {
            modsAlreadyRequested.remove(channel);
        }
    }
    
    private String prepareAndCheckParameters(String regex, String parameters) {
        if (parameters == null) {
            return null;
        }
        parameters = parameters.trim();
        return parameters.matches(regex) ? parameters : null;
    }
    
}
