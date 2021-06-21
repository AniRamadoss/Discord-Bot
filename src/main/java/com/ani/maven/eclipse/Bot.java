package com.ani.maven.eclipse;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.*;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

/**
 * Discord Bot
 * 
 * @author Aniruthan Ramadoss
 *
 */
public class Bot {
    private final GatewayDiscordClient client;
    /**
     * Hashmap that contains all the commands and the mapping to their
     * functionalities.
     */
    public static final Map<String, Command> commands = new HashMap<>();
    /**
     * Logs the login time of the bot
     */
    public static final Instant LOGIN_TIME = Instant.now();
    /**
     * Contains all the message matches saved to the bot.
     */
    public static Map<String, String> matches = new HashMap<>();
    /**
     * Whether deleted messages are allowed to be logged or not.
     */
    public static boolean allowDeleted = true;

    /**
     * The command command prefix. By default it is "!" but it can be changed to
     * anything, including words.
     */
    public static String prefix = "!";

    /**
     * Creates the bot and sets up the commands.
     */
    public Bot() {
        messageMatch();
        setUpVoiceCommands();
        reminder();
        prefixChange();
        help();
        toggleDelete();

        Scanner file = null;
        try {
            file = new Scanner(new File("BOT_KEY.txt"));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String key = file.nextLine();

        client = DiscordClientBuilder.create(key).build().login().block();

        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(
            event -> {

                final String content = event.getMessage().getContent()
                    .toLowerCase();

                for (final Map.Entry<String, Command> entry : commands
                    .entrySet()) {

                    if (content.startsWith(prefix + entry.getKey())) {

                        if (entry.getKey().contains("reminder")) {
                            new Thread(() -> {
                                entry.getValue().execute(event);
                            }).start();
                        }
                        else {
                            entry.getValue().execute(event);
                        }

                        break;
                    }

                }

                respondToMessage(content, event);

            });

        client.getEventDispatcher().on(MessageDeleteEvent.class).subscribe(
            event -> {
                // If statement that prevents bot from crashing when a message
                // that is not in the cache is deleted
                if (event.getMessage().isPresent() && allowDeleted) {
                    final String deletedMsg = event.getMessage().get()
                        .getContent();

                    Member member = event.getMessage().get().getAuthorAsMember()
                        .block();
                    String output = member.getDisplayName() + " AKA " + member
                        .getUsername() + " deleted a message in " + event
                            .getMessage().get().getChannel().block()
                            .getMention() + "\n**" + deletedMsg + "**";
                    event.getMessage().get().getChannel().block().createMessage(
                        output).block();
                }

            });

        client.onDisconnect().block();
    }


    /**
     * Toggles whether or not the bot will log deleted messages.
     */
    public void toggleDelete() {
        commands.put("delete", event -> {
            allowDeleted = !allowDeleted;
            String message = "Deleted Message Logger is now turned **on**.";
            if (!allowDeleted) {
                message = "Deleted Message Logger is now turned **off**.";
            }
            event.getMessage().getChannel().block().createMessage(message)
                .block();
        });
    }


    /**
     * Provides information to the user about bot functionalities.
     */
    public void help() {
        commands.put("help", event -> {
            String helpMessage =
                "Deleted Messages Logger: sends deleted messages in the channel they were deleted in as soon as they are deleted. This feature can be toggled on and off through the *" + prefix + "delete* command."
                    + "\nMessageMatcher: Configure the bot to respond to a message with another message.  For example *" + prefix + "match ping ANDD pong* makes the bot respond with 'pong' whenever a message containing the word 'ping' is sent."
                    + "\nReminder Service: The user can schedule a reminder through the use of the !reminder command. Type *" + prefix + "reminder* for more information about the syntax."
                    + "\nPrefix Change: Change the command prefix to other symbols through the *" + prefix + "prefix* command. By default, it is ! but it can be changed to any character or word.";
            event.getMessage().getChannel().block().createMessage(helpMessage)
                .block();
        });
    }


    /**
     * Changes the command prefix. By default it is '!'
     */
    public void prefixChange() {
        commands.put("prefix", event -> {
            String msg = event.getMessage().getContent();
            String[] contents = msg.split(" ");

            if (contents.length < 2) {
                event.getMessage().getChannel().block().createMessage(
                    "Invalid syntax. \n Use " + prefix
                        + "change *symbol identifier*)").block();
            }

            else {
                prefix = contents[1];
                event.getMessage().getChannel().block().createMessage(
                    "Prefix set!  An example command would now be \n"
                        + prefix + "reminder 10 seconds do homework")
                    .block();
            }

        });
    }


    /**
     * Saves the message matches into the matches hashmap.
     */
    public void messageMatch() {
        commands.put("match", event -> {
            String msg = event.getMessage().getContent();
            String[] contents = msg.split(" ANDD ");
            if (contents.length < 2) {
                event.getMessage().getChannel().block().createMessage(
                    "Invalid syntax. \n Use " + prefix
                        + "match *message* ANDD *response*").block();
            }

            else {
                matches.put(contents[0].substring(7), contents[1]);
                event.getMessage().getChannel().block().createMessage(
                    "Message Match Created!").block();
            }
        });

    }


    /**
     * The bot filters the created message and searches for any trigger words
     * that are contained in the matches hashmap. It responds accordingly if it
     * does.
     * 
     * @param content
     *            The message text to filter.
     * @param event
     *            The message creation event
     */
    public void respondToMessage(String content, MessageCreateEvent event) {
        for (String key : matches.keySet()) {
            if (content.contains(key) && !content.contains("ANDD") && !content
                .contains(prefix + "match") && !event.getMember().get()
                    .getNicknameMention().equals("<@!852649504691191878>")) {
                event.getMessage().getChannel().block().createMessage(matches
                    .get(key)).block();

            }
        }

    }


    /**
     * Schedules a reminder in a given amount of time.
     * 
     */
    public void reminder() {
        commands.put("reminder", event -> {

            final Member member = event.getMember().orElse(null);

            final String content = event.getMessage().getContent()
                .toLowerCase();
            String[] contents = content.split(" ");

            String output = "";
            double num = 0;
            boolean valid = true;
            // Checks if the input passed in is parsable.
            if (contents.length > 2 && contents[1].matches("-?(0|[1-9]\\d*)")) {
                num = Integer.parseInt(contents[1]);
            }

            else {
                output = "Invalid syntax! The proper syntax is\n**"
                    + prefix
                    + "reminder (positive integer) (second, minute, hour, day, or week) (message)**\nDon't include parantheses";
                valid = false;
            }

            if (valid) {
                if (contents[2].charAt(contents[2].length() - 1) == 's') {
                    contents[2] = contents[2].substring(0, contents[2].length()
                        - 1);
                }

                if (!(contents[2].equals("second") || contents[2].equals(
                    "minute") || contents[2].equals("hour") || contents[2]
                        .equals("day") || contents[2].equals("week"))) {
                    valid = false;
                }
            }

            if (valid) {

                event.getMessage().getChannel().block().createMessage(
                    "Reminder set!").block();

                long reminderTime = 0;

                if (contents[2].equals("second")) {
                    reminderTime = (long)(1000l * num);
                }

                else if (contents[2].equals("minute")) {
                    reminderTime = (long)(60000l * num);
                }

                else if (contents[2].equals("hour")) {
                    reminderTime = (long)(3600000l * num);
                }

                else if (contents[2].equals("day")) {
                    reminderTime = (long)(86400000l * num);
                }

                else if (contents[2].equals("week")) {
                    reminderTime = (long)(604800000l * num);
                }

                // Suspend the thread until the timer finishes
                try {
                    Thread.sleep(reminderTime);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String message = "";
                for (int i = 3; i < contents.length; i++) {
                    message += contents[i] + " ";
                }

                output = "Hey " + member.getNicknameMention() + " " + message;

            }

            event.getMessage().getChannel().block().createMessage(output)
                .block();

        });

    }


    /**
     * This method's code is partially taken from the Discord4J tutorial.
     * Sets up commands to join, leave, and play music in voice channels.
     */
    public void setUpVoiceCommands() {
        // Creates AudioPlayer instances and translates URLs to AudioTrack
        // instances
        final AudioPlayerManager playerManager =
            new DefaultAudioPlayerManager();

        playerManager.getConfiguration().setFrameBufferFactory(
            NonAllocatingAudioFrameBuffer::new);

        // Allow playerManager to parse YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);

        final AudioPlayer player = playerManager.createPlayer();

        AudioProvider provider = new AudioPlayerProvider(player);

        commands.put("join", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel()
                        .block();
                    if (channel != null) {
                        channel.join(spec -> spec.setProvider(provider))
                            .block();
                    }
                }
            }
        });

        final TrackScheduler scheduler = new TrackScheduler(player);
        commands.put("play", event -> {
            final String content = event.getMessage().getContent();
            final List<String> command = Arrays.asList(content.split(" "));
            playerManager.loadItem(command.get(1), scheduler);
        });

        commands.put("leave", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel()
                        .block();
                    if (channel != null) {
                        channel.sendDisconnectVoiceState().block();
                    }
                }
            }
        });
    }

}




/**
 * Interface that is used to build lambda expressions in this class.
 * 
 * @author Aniruthan Ramadoss
 *
 */
interface Command {
    void execute(MessageCreateEvent event);
}
