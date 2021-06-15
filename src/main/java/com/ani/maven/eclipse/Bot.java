package com.ani.maven.eclipse;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.gateway.intent.IntentSet;
import discord4j.voice.AudioProvider;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import discord4j.common.ResettableInterval;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

public class Bot {
    private final GatewayDiscordClient client;
    private static final Map<String, Command> commands = new HashMap<>();

    public Bot() {

        setUpVoiceCommands();

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
                    // Using ! as "prefix" to any command in the
                    // system.
                    if (content.startsWith('!' + entry.getKey())) {
                        entry.getValue().execute(event);
                        break;
                    }
                }
            });

        addResponseToMessage("keqing", "best!");
        reminder();
        client.onDisconnect().block();
    }


    public void addResponseToMessage(String msg, String response) {
        commands.put(msg, event -> event.getMessage().getChannel().block()
            .createMessage(response).block());
    }


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

        AudioProvider provider = new LavaPlayerAudioProvider(player);

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

            double num = 0;
            boolean valid = true;
            try {
                num = Integer.parseInt(contents[1]);

            }
            catch (Exception e) {
                event.getMessage().getChannel().block().createMessage(
                    "Invalid syntax! The proper syntax is\n!reminder 1 **second, minute, hour, day, or week** (message)")
                    .block();
                valid = false;
            }

            if (contents[2].charAt(contents[2].length() - 1) == 's') {
                contents[2] = contents[2].substring(0, contents[2].length()
                    - 1);
            }

            if (!(contents[2].equals("second") || contents[2].equals("minute")
                || contents[2].equals("hour") || contents[2].equals("day")
                || contents[2].equals("week"))) {
                valid = false;
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

                System.out.println(reminderTime);

                System.out.println("Times up");

                String message = "";
                for (int i = 3; i < contents.length; i++) {
                    message += contents[i] + " ";
                }

                event.getMessage().getChannel().block().createMessage("Hey "
                    + member.getNicknameMention() + " " + message).block();
            }

        });

    }

}




interface Command {
    void execute(MessageCreateEvent event);
}
