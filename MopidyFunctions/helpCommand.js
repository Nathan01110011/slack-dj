async function helpCommand() {
  var helpInstructions =
    `Hello! You can tell me what to do with the following commands!` +
    `\n\n *play* "_insert search term_" : Queue up a specfic song - respond with a yes or no.` +
    `\n *music* "_pause/play_" : Start or stop playback ` +
    `\n *what* : I'll tell you what's currently playing` +
    `\n *next* : Show's the current track list` +
    `\n *belter* : Queues up an absolute belter` +
    `\n *quote* : One of my classic quotes`;

  return helpInstructions;
}

module.exports.helpCommand = helpCommand;
