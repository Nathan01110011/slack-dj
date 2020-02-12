// TODO : UP NEXT

async function showPlaying(mopidy) {
  let currentlyPlaying;
  try {
    const track = await mopidy.playback.getCurrentTrack();
    const state = await mopidy.playback.getState();
    if (track) {
      currentlyPlaying =
        track.name +
        ` by ` +
        track.artists[0].name +
        ` is currently ` +
        state +
        `!`;
    } else {
      currentlyPlaying = `No music is currently playing!`;
    }
  } catch (e) {
    currentlyPlaying = `Don't know what's happened here, try again later ¯\\_(ツ)_/¯`;
  }
  return currentlyPlaying;
}

module.exports.showPlaying = showPlaying;
