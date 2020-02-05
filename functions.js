async function setMusicState(newState, mopidy) {
  if (newState === "pause") {
    await mopidy.playback.pause().catch();
    return "Music now not playing!";
  } else if (newState === "play") {
    await mopidy.playback.play().catch();
    return "Music now playing!";
  }
}

async function addSong(options, mopidy) {
  const tracks = await mopidy.library
    .search({
      query: {
        track_name: options
      }
    })
    .catch();

  const trackURI = tracks[0].tracks[0].uri;
  mopidy.tracklist.add({ uris: [trackURI] });

  const currentState = await mopidy.playback.getState().catch();
  console.log(currentState);
  if (currentState === "stopped") {
    await mopidy.playback.play().catch();
  }
  return "Song added to queue.";
}

async function showPlaying(mopidy) {
  let currentlyPlaying;
  try {
    const track = await mopidy.playback.getCurrentTrack();
    if (track) {
      currentlyPlaying =
        "Currently playing: " + track.name + " by " + track.artists[0].name;
    } else {
      currentlyPlaying = "No music is currently playing!";
    }
  } catch (e) {
    console.warn("Something went wrong", e);
    currentlyPlaying =
      "Don't know what's happened here, try again later ¯\\_(ツ)_/¯";
  }
  return currentlyPlaying;
}

module.exports.setMusicState = setMusicState;
module.exports.addSong = addSong;
module.exports.showPlaying = showPlaying;
