// TODO: Add check for state

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

module.exports.showPlaying = showPlaying;
