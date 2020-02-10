// TODO: Possibly create new directory and keep each of these commands in a separate file

async function setMusicState(newState, mopidy) {
  if (newState === "pause") {
    await mopidy.playback.pause().catch();
    return "Music now *_not_* playing!";
  } else if (newState === "play") {
    await mopidy.playback.play().catch();
    return "Music now playing!";
  } else {
    return "Unrecognised command.";
  }
}

async function addSong(options, mopidy) {
  const tracks = await mopidy.library
    .search({
      query: {
        track_name: options
      }
    })
    .catch(function(error) {
      return "Something is broke :(";
    });

  if (tracks[0].tracks === undefined) {
    return "Something is broke :(";
  }

  const track = tracks[0].tracks[0];
  var trackSelected = track.name + " by " + track.artists[0].name;
  mopidy.tracklist.add({ uris: [track.uri] });

  const currentState = await mopidy.playback.getState().catch(function(error) {
    return "Something is broke :(";
  });
  if (currentState === "stopped") {
    await mopidy.playback.play().catch();
  }
  return trackSelected + " added to queue.";
}

async function playBelter(options, mopidy) {
  const belters = await mopidy.library
    .search({
      query: { uri: ["spotify:playlist:3xVCCnIZV6a2gvmvcYWOSP"] },
      uris: ["spotify:"]
    })
    .catch(function(error) {
      return "Something is broke :(";
    });

  const beltersSize = belters[0].tracks.length;
  const belterSelect = Date.now() % (beltersSize - 1);

  const track = belters[0].tracks[belterSelect];
  var trackSelected;
  if (track) {
    mopidy.tracklist.add({ uris: [track.uri] });
    trackSelected = "Coming up: " + track.name + " by " + track.artists[0].name;
  }
  return trackSelected;
}

async function showPlaying(options, mopidy) {
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
module.exports.playBelter = playBelter;
