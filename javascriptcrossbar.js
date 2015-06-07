(function(){
  "use strict"

  var baseImage = 'MINOU_IMAGE_BASE:';
  var connection = new autobahn.Connection({
    url: location.origin.replace(/^http/, 'ws') + '/ws',
    realm: 'minou'
  });

  var number = 0;
  var messageTpl = _.template($('#message-tpl').text());


  connection.onopen = function(session){
    $('#input-box').removeAttr('disabled');

    session.subscribe('minou.montreal', function(args, kwargs) {
      console.log(arguments);
      appendMessage(kwargs.from, kwargs.content, kwargs.fake_name, kwargs.picture);
    });

    function appendMessage(from, content, fake_name, picture) {

      if(typeof picture !== 'undefined'){
        var image = new Image();
        image.src = 'data:image/png;base64,' + picture;
        $('#messages').append(image);
      } else{
        $('#messages').append(messageTpl({from: fake_name, content: content}));
      }
    }

    function sendMessage(message) {
      session.publish('minou.montreal', [], {from:'3ui45389fj43', fake_name:'Cat Searching' , content: 'hey' + number});
	  number = number + 1;
      appendMessage('self', message);
    }

    $('#input-form').submit(function(event){
      event.preventDefault()
      sendMessage($('#input-box').val());
      $('#input-box').val('');
    });
  };

  connection.open();
})()