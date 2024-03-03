package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class SpotifyRepository {
    public HashMap<Artist, List<Album>> artistAlbumMap;
    public HashMap<Album, List<Song>> albumSongMap;
    public HashMap<Playlist, List<Song>> playlistSongMap;
    public HashMap<Playlist, List<User>> playlistListenerMap;
    public HashMap<User, Playlist> creatorPlaylistMap;
    public HashMap<User, List<Playlist>> userPlaylistMap;
    public HashMap<Song, List<User>> songLikeMap;

    public List<User> users;
    public List<Song> songs;
    public List<Playlist> playlists;
    public List<Album> albums;
    public List<Artist> artists;

    public SpotifyRepository() {
        //To avoid hitting apis multiple times, initialize all the hashmaps here with some dummy data
        artistAlbumMap = new HashMap<>();
        albumSongMap = new HashMap<>();
        playlistSongMap = new HashMap<>();
        playlistListenerMap = new HashMap<>();
        creatorPlaylistMap = new HashMap<>();
        userPlaylistMap = new HashMap<>();
        songLikeMap = new HashMap<>();

        users = new ArrayList<>();
        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
    }

    public User createUser(String name, String mobile) {
        User newUser = new User(name, mobile);
        users.add(newUser);
        return newUser;
    }

    public Artist createArtist(String name) {
        Artist newArtist = new Artist(name);
        artists.add(newArtist);
        return newArtist;
    }

    public Album createAlbum(String title, String artistName) {
        Artist artist = findArtistByName(artistName);
        if (artist == null) {
            artist = createArtist(artistName);
        }
        Album newAlbum = new Album(title);
        albums.add(newAlbum);

        List<Album> artistAlbums = artistAlbumMap.getOrDefault(artist, new ArrayList<>());
        artistAlbums.add(newAlbum);
        artistAlbumMap.put(artist, artistAlbums);

        return newAlbum;
    }


    public Song createSong(String title, String albumName, int length) throws Exception {
        Album album = findAlbumByTitle(albumName);
        if (album == null) {
            throw new Exception("Album does not exist");
        }

        Song newSong = new Song(title, length);
        songs.add(newSong);

        List<Song> albumSongs = albumSongMap.getOrDefault(album, new ArrayList<>());
        albumSongs.add(newSong);
        albumSongMap.put(album, albumSongs);

        return newSong;
    }

    public Playlist createPlaylistOnLength(String mobile, String title, int length) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        Playlist newPlaylist = new Playlist(title);
        playlists.add(newPlaylist);

        List<Song> playlistSongs = new ArrayList<>();
        for (Song song : songs) {
            if (song.getLength() == length) {
                playlistSongs.add(song);
            }
        }

        playlistSongMap.put(newPlaylist, playlistSongs);
        playlistListenerMap.put(newPlaylist, Collections.singletonList(user));
        creatorPlaylistMap.put(user, newPlaylist);

        return newPlaylist;
    }

    public Playlist createPlaylistOnName(String mobile, String title, List<String> songTitles) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        Playlist newPlaylist = new Playlist(title);
        playlists.add(newPlaylist);

        List<Song> playlistSongs = new ArrayList<>();
        for (String songTitle : songTitles) {
            Song song = findSongByTitle(songTitle);
            if (song != null) {
                playlistSongs.add(song);
            }
        }

        playlistSongMap.put(newPlaylist, playlistSongs);
        playlistListenerMap.put(newPlaylist, Collections.singletonList(user));
        creatorPlaylistMap.put(user, newPlaylist);

        return newPlaylist;
    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        Playlist playlist = null;
        for (Playlist p : playlists) {
            if (p.getTitle().equals(playlistTitle)) {
                playlist = p;
                break;
            }
        }

        if (playlist == null) {
            throw new Exception("Playlist does not exist");
        }

        List<User> listeners = playlistListenerMap.getOrDefault(playlist, new ArrayList<>());
        if (!listeners.contains(user) && !playlist.equals(creatorPlaylistMap.get(user))) {
            listeners.add(user);
            playlistListenerMap.put(playlist, listeners);
        }

        return playlist;
    }

    public String mostPopularArtist() {
        String mostPopularArtist = null;
        int maxLikes = Integer.MIN_VALUE;

        for (Artist artist : artists) {
            if (artist.getLikes() > maxLikes) {
                mostPopularArtist = artist.getName();
                maxLikes = artist.getLikes();
            }
        }

        return mostPopularArtist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        Song song = findSongByTitle(songTitle);
        if (song == null) {
            throw new Exception("Song does not exist");
        }

        List<User> likedUsers = songLikeMap.getOrDefault(song, new ArrayList<>());
        if (!likedUsers.contains(user)) {
            likedUsers.add(user);
            songLikeMap.put(song, likedUsers);

            // Auto-like the corresponding artist
            String artistName = getArtistNameFromSong(song);
            if (artistName != null) {
                Artist artist = findArtistByName(artistName);
                if (artist != null) {
                    artist.setLikes(artist.getLikes() + 1);
                }
            }

            // Update song likes
            song.setLikes(song.getLikes() + 1);
        }

        return song;
    }

    public String mostPopularSong() {
        String mostPopularSong = null;
        int maxLikes = Integer.MIN_VALUE;

        for (Song song : songs) {
            if (song.getLikes() > maxLikes) {
                mostPopularSong = song.getTitle();
                maxLikes = song.getLikes();
            }
        }

        return mostPopularSong;
    }

    private Artist findArtistByName(String name) {
        return artists.stream()
                .filter(artist -> artist.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Album findAlbumByTitle(String title) {
        return albums.stream()
                .filter(album -> album.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    private Song findSongByTitle(String title) {
        return songs.stream()
                .filter(song -> song.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    private User findUserByMobile(String mobile) {
        return users.stream()
                .filter(user -> user.getMobile().equals(mobile))
                .findFirst()
                .orElse(null);
    }

    private String getArtistNameFromSong(Song song) {
        Album album = albumSongMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(song))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);

        return artistAlbumMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(album))
                .findFirst()
                .map(Map.Entry::getKey)
                .map(Artist::getName)
                .orElse(null);
    }

}




