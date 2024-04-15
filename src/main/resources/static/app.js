const { webSocket } = rxjs.webSocket;
var socketConnection = webSocket('ws://localhost:8080/socket');
var figure = "";
var gameLoaded = false;

$(document).ready(function () {
    console.log('ready');
    $( "#target" ).on( "click", function() {
      alert( "Handler for `click` called." );
      console.log("hello");
    } );
    /*setTimeout(function(){
      $(document).on('click','#add_external_link',function(){
          alert('clicked');
      });
   },1000);*/
});

function initialize() {
    if (gameLoaded) return;
    gameLoaded = true;
    console.log("Hello")
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

    socketConnection.subscribe({
                       next: msg =>handleNext(msg), // Called whenever there is a message from the server.
                       error: err => console.log(err), // Called if at any point WebSocket API signals some kind of error.
                       complete: () => console.log('complete') // Called when connection is closed (for whatever reason).
                      });
}

function clickImageHandler(clicked_id) {
    console.log("Clicked " + clicked_id)
    socketConnection.next(createMessage(clicked_id, "CLIENT_CLICK"));
}

function createMessage(text, type) {
    return {"text": text, "type": type};
}

function handleNext(msg) {
    console.log(msg);
    if (msg.type === "FIGURE") {
        figure = msg.text;
    } else if (msg.type === "SHOW") {
        $(`#${msg.text} .img-responsive`).attr("src",`images/${figure}.jpg`);
    } else if (msg.type === "INFO") {
        console.log("Got an info!")
        $("h2").text(msg.text);
    }
}
