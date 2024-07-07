var width = (window.innerWidth > 0) ? window.innerWidth : document.documentElement.clientWidth;
if (width > 900) {
    this.inputEditor = CodeMirror.fromTextArea(document.getElementById("source"), {
        lineNumbers: false, mode: "text/bau"
    });
    setInterval(function () {
        inputEditor.save();
    }, 200)
}