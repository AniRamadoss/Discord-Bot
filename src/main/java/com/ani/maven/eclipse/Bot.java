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
import reactor.core.scheduler.Scheduler;
import discord4j.common.ResettableInterval;
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

        client = DiscordClientBuilder.create(
            "ODUyNjQ5NTA0NjkxMTkxODc4.YMJ5uw.Iqyq6rXrRaHNEJnBBPkYazzFteI")
            .build().login().block();
        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(
            event -> {
                // Message.getContent() is a String
                final String content = event.getMessage().getContent();

                for (final Map.Entry<String, Command> entry : commands
                    .entrySet()) {
                    // Using ! as our "prefix" to any command in the
                    // system.
                    if (content.startsWith('!' + entry.getKey())) {
                        entry.getValue().execute(event);
                        break;
                    }
                }
            });

        final AudioPlayerManager playerManager =
            new DefaultAudioPlayerManager();

        playerManager.getConfiguration().setFrameBufferFactory(
            NonAllocatingAudioFrameBuffer::new);

        // Parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);

        // Create an AudioPlayer
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
                        // join returns a VoiceConnection which would be
                        // required if we were
                        // adding disconnection features, but for now we are
                        // just ignoring it.
                        channel.join(spec -> spec.setProvider(provider))
                            .block();
                        final TrackScheduler scheduler = new TrackScheduler(
                            player);
                        commands.put("play", event2 -> {
                            final String content = event.getMessage()
                                .getContent();
                            final List<String> command = Arrays.asList(content
                                .split(" "));
                            playerManager.loadItem(command.get(1), scheduler);
                        });
                    }

                }
            }
        });

        commands.put("keqing", event -> event.getMessage().getChannel().block()
            .createMessage("best!").block());

        client.onDisconnect().block();
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
