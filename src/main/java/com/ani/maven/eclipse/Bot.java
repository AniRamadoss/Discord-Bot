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

        //wip
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


    public void logRemovedMessage(MessageDeleteEvent event) {
        // Message msg = event.getMessage().orElse(null);
        // if (msg == null) {
        // // No Action taken
        // return;
        // }
        // // Logs the deleted message in the channel
        // createMessageInChannel(event.getChannel().block(), event.getMessage()
        // .get().getUserData().toString() + " deleted the message: " + event
        // .getMessage().toString());

        // client.getEventDispatcher().on(MessageCreateEvent.class).map(
        // MessageCreateEvent::getMessage);
        // event.getChannel().flatMap(messageChannel -> messageChannel
        // .createMessage(event.getMessage()));
    }


    public void reminder() {
        // Scheduler scheduler = new Scheduler();
        // ResettableInterval timer = new ResettableInterval();
    }

}




interface Command {
    void execute(MessageCreateEvent event);
}
