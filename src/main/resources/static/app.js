const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8081/ws'
});

stompClient.onConnect = (frame) => {
    setConnected(true);
    alert('Connected: ' + frame);
    stompClient.subscribe('/topic/responses', (message) => {
        showResponse(JSON.parse(message.body).content);
    });
};

stompClient.onWebSocketError = (error) => {
    alert('An error occured: ' + error);
};

stompClient.onStompError = (frame) => {
    alert('Broker reported error: ' + frame.headers['message']);
    alert('Additional details: ' + frame.body)
};

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);

    if (connected) {
        $("#conversation").show();
    } else {
        $("#conversation").hide();
        $("#responses").html("");
    }
}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate().then();
    setConnected(false);
    alert("Successfully disconnected.");
}

function sendMessage() {
    stompClient.publish({
        destination: "/app/messages",
        body: JSON.stringify({'content': $("#message").val()})
    });
}

function showResponse(message) {
    $("#responses").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $("#connect").click(() => connect());
    $("#disconnect").click(() => disconnect());
    $("#send").click(() => sendMessage());
})