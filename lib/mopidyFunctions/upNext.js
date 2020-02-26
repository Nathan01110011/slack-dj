async function upNext(mopidy) {
  const list = await mopidy.tracklist.getTracks();
  const currentTrackIndex = await mopidy.tracklist.index();
  const array = list.splice(currentTrackIndex + 1);
  console.log(array);
  if (array.length < 1) {
    return "Nothing queued at the minute.";
  }

  var tracksUpNext = "";
  var loopCounter = 1;
  for (const Object of array) {
    tracksUpNext = tracksUpNext.concat(
      loopCounter + ") " + Object.name + ` by ` + Object.artists[0].name + "\n"
    );
    loopCounter++;
  }

  return tracksUpNext;
}

module.exports.upNext = upNext;
