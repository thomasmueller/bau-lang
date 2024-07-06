this.inputEditor = CodeMirror.fromTextArea(document.getElementById("source"), {
    lineNumbers: false, mode: "text/bau"
});
setInterval(function () {
    inputEditor.save();
}, 200)