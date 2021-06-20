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

public class Bot {
    private final GatewayDiscordClient client;
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Instant LOGIN_TIME = Instant.now();
    private static Map<String, String> matches = new HashMap<>();

    public Bot() {

        setUpVoiceCommands();
        reminder();

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
                String[] contents = content.split(" ANDD ");

                for (final Map.Entry<String, Command> entry : commands
                    .entrySet()) {
                    // Using ! as "prefix" to any command in the
                    // system.
                    if (content.startsWith('!' + entry.getKey())) {

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

                respondToMessage(contents, event);

            });

        client.getEventDispatcher().on(MessageDeleteEvent.class).subscribe(
            event -> {
                // If statement that prevents bot from crashing when a message
                // that is not in the cache is deleted
                if (event.getMessage().isPresent()) {
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


    public void messageMatch() {
        commands.put("match", event -> addResponseToMessage(event));
    }


    public void addResponseToMessage(MessageCreateEvent event) {
        String msg = event.getMessage().getContent();
        String[] contents = msg.split(" ANDD ");
        boolean found = false;
        int pivot = 0;

        if (contents.length < 1) {
            event.getMessage().getChannel().block().createMessage(
                "Invalid syntax. \n Use !match *message* ANDD *response*");
        }

        else {
            matches.put(contents[0], contents[1]);
            event.getMessage().getChannel().block().createMessage(
                "Message Match Created!");
        }

    }


    public void respondToMessage(String[] contents, MessageCreateEvent event) {
        if (contents.length > 0) {
            String key = contents[0].substring(6);
            if (matches.containsKey(key)) {
                event.getMessage().getChannel().block().createMessage(matches
                    .get(key));
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
            // Checks if the input passed in is parseable.
            if (contents[1].matches("-?(0|[1-9]\\d*)")) {
                num = Integer.parseInt(contents[1]);
            }

            else {

                output =
                    "Invalid syntax! The proper syntax is\n**!reminder (positive integer) (second, minute, hour, day, or week) (message)**\nDon't include parantheses";
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




interface Command {
    void execute(MessageCreateEvent event);
}
