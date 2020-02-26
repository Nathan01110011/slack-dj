async function setMusicState(mopidy, options) {
  if (options === undefined) {
    return `What do you want to do with the music? Play/Pause?`;
  }
  if (options === `pause`) {
    await mopidy.playback.pause().catch();
    return `Music now *_not_* playing!`;
  } else if (options === `play`) {
    await mopidy.playback.play().catch();
    return `Music now playing!`;
  } else {
    return `Unrecognised command.`;
  }
}

module.exports.setMusicState = setMusicState;
