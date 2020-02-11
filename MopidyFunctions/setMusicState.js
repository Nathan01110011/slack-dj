async function setMusicState(newState, mopidy) {
  if (newState === `pause`) {
    await mopidy.playback.pause().catch();
    return `Music now *_not_* playing!`;
  } else if (newState === `play`) {
    await mopidy.playback.play().catch();
    return `Music now playing!`;
  } else {
    return `Unrecognised command.`;
  }
}

module.exports.setMusicState = setMusicState;
