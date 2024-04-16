const { webSocket } = rxjs.webSocket;
var socketConnection = webSocket('SOCKET_HOST');
var figure = "";
var gameState = "NO_GAME";
var errorFlag = false;
var standardBackgroundColor = "#aa88b9";

function initialize() {
    if (gameState === "IN_GAME") return;

    if (gameState === "NO_GAME") {
        initializeCells();
    } else if (gameState === "END_GAME") {
        resetCells();
    }
    $("h2").css("background", standardBackgroundColor);
    gameState = "IN_GAME";
    errorFlag = false;
    connectSocket();
}

function connectSocket() {
    socketConnection.subscribe({
        next: msg => handleNext(msg), // Called whenever there is a message from the server.
        error: err => handleError(err), // Called if at any point WebSocket API signals some kind of error.
        complete: () => handleComplete() // Called when connection is closed (for whatever reason).
    });
}

function clickImageHandler(clicked_id) {
    console.log("Clicked " + clicked_id)
    socketConnection.next(createMessage(clicked_id, "CLIENT_CLICK"));
}

function createMessage(text, type) {
    return { "text": text, "type": type };
}

function handleNext(msg) {
    console.log(msg);
    if (msg.type === "FIGURE") {
        figure = msg.text;
    } else if (msg.type === "SHOW") {
        $(`#${msg.text} .img-responsive`).attr("src", `images/${figure}.jpg`);
    } else if (msg.type === "INFO") {
        console.log("Got an info!")
        $("h2").html(msg.text);
    }
}

function handleError(err) {
    console.log(err);
    if (errorFlag) return;
    errorFlag = true;
    delayedEndGame();
    setTimeout(() => {
        if (errorFlag) {
            $("h2").html("Tap here to play again!");
        }
    }, 20000);
}

function handleComplete() {
    console.log('complete');
    delayedEndGame();
}

function delayedEndGame() {
    setTimeout(() => {
        $("h2").css("background", "darkseagreen");
        gameState = "END_GAME"
    }, 2000);
}

function initializeCells() {
    $(".container").append(`
        <div class="row mb-3" style="padding-right: 15px; padding-left: 15px;"></div>
    `)
    for (let i = 0; i < 9; i++) {
        $(".row").append(`
            <div id=${i} onClick="clickImageHandler(this.id)" class="col-xs-4 themed-grid-col">
                <img src="images/empty-cell.jpg" class="img-responsive center-block">
            </div>
        `)
    }
}

function resetCells() {
    for (let i = 0; i < 9; i++) {
        $(`#${i} .img-responsive`).attr("src", `images/empty-cell.jpg`);
    }
}
