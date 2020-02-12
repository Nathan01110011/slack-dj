async function playBelter(mopidy) {
  const belters = await mopidy.library
    .search({
      query: { uri: [`spotify:playlist:3xVCCnIZV6a2gvmvcYWOSP`] },
      uris: [`spotify:`]
    })
    .catch(function(error) {
      return `Something is broke :(`;
    });

  const belterSelect = Date.now() % (belters[0].tracks.length - 1);

  const track = belters[0].tracks[belterSelect];
  var trackSelected;
  if (track) {
    mopidy.tracklist.add({ uris: [track.uri] });
    trackSelected = `Coming up: ` + track.name + ` by ` + track.artists[0].name;
  }
  return trackSelected;
}

module.exports.playBelter = playBelter;
