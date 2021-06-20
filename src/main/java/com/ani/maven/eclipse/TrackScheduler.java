package com.ani.maven.eclipse;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Not my code
 * 
 *
 */
public final class TrackScheduler implements AudioLoadResultHandler  {

    private final AudioPlayer player;

    public TrackScheduler(final AudioPlayer player) {
        this.player = player;
    }


    public void trackLoaded(final AudioTrack track) {
        player.playTrack(track);
    }


    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void noMatches() {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void loadFailed(FriendlyException exception) {
        // TODO Auto-generated method stub
        
    }

}
